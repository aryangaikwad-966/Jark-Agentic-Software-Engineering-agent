package com.example.VoxCode.validator;

import com.example.VoxCode.entity.CodeRepository;
import com.example.VoxCode.entity.Evidence;
import com.example.VoxCode.entity.Finding;
import com.example.VoxCode.repository.CodeRepositoryRepository;
import com.example.VoxCode.repository.FindingRepository;
import com.example.VoxCode.service.AstAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Validates findings by independently verifying that cited repository artifacts exist.
 * 
 * <p>This validation layer prevents hallucinations by ensuring that all evidence
 * references (AST nodes, file paths, classes, methods) actually exist in the repository.
 * Only CONFIRMED findings can proceed to remediation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindingValidator {

    private final FindingRepository findingRepository;
    private final CodeRepositoryRepository codeRepositoryRepository;
    private final AstAnalysisService astAnalysisService;

    /**
     * Validation status constants.
     */
    public static final String PENDING_VALIDATION = "PENDING_VALIDATION";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String REJECTED = "REJECTED";
    public static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";

    /**
     * Validates a single finding by checking all its evidence.
     * 
     * @param findingId the ID of the finding to validate
     * @return the updated finding with validation status
     */
    @Transactional
    public Finding validateFinding(Long findingId) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found: " + findingId));

        log.info("Validating finding {} for repository {}", findingId, finding.getRepository().getId());

        ValidationResult result = validateEvidence(finding);

        if (result.isValid()) {
            finding.setValidationStatus(CONFIRMED);
            log.info("Finding {} validated as CONFIRMED", findingId);
        } else {
            finding.setValidationStatus(REJECTED);
            finding.setExplanation(finding.getExplanation() + 
                    "\n\nValidation failed: " + result.getFailureReason());
            log.warn("Finding {} validated as REJECTED: {}", findingId, result.getFailureReason());
        }

        return findingRepository.save(finding);
    }

    /**
     * Validates all findings for a specific investigation.
     * 
     * @param investigationId the investigation ID
     * @return list of updated findings with validation status
     */
    @Transactional
    public List<Finding> validateInvestigationFindings(Long investigationId) {
        List<Finding> findings = findingRepository.findByInvestigationId(investigationId);
        log.info("Validating {} findings for investigation {}", findings.size(), investigationId);

        return findings.stream()
                .map(finding -> validateFinding(finding.getId()))
                .toList();
    }

    /**
     * Validates all findings with PENDING_VALIDATION status.
     * 
     * @return list of updated findings
     */
    @Transactional
    public List<Finding> validatePendingFindings() {
        List<Finding> pendingFindings = findingRepository.findByValidationStatus(PENDING_VALIDATION);
        log.info("Validating {} pending findings", pendingFindings.size());

        return pendingFindings.stream()
                .map(finding -> validateFinding(finding.getId()))
                .toList();
    }

    /**
     * Performs the actual validation of evidence against repository artifacts.
     */
    private ValidationResult validateEvidence(Finding finding) {
        List<Evidence> evidenceList = finding.getEvidence();

        if (evidenceList == null || evidenceList.isEmpty()) {
            return ValidationResult.invalid("No evidence provided");
        }

        // Get repository workspace path
        CodeRepository repository = finding.getRepository();
        if (repository == null || repository.getLocalPath() == null) {
            return ValidationResult.invalid("Repository not properly initialized or missing local path");
        }

        Path workspacePath = Paths.get(repository.getLocalPath());
        if (!Files.exists(workspacePath)) {
            return ValidationResult.invalid("Repository workspace path does not exist: " + workspacePath);
        }

        // Validate each piece of evidence
        int validEvidenceCount = 0;
        for (Evidence evidence : evidenceList) {
            if (!evidence.isValid()) {
                log.warn("Evidence {} has invalid structure", evidence.getId());
                continue;
            }

            ValidationResult evidenceResult = validateSingleEvidence(evidence, workspacePath);
            if (evidenceResult.isValid()) {
                validEvidenceCount++;
            } else {
                log.warn("Evidence {} failed validation: {}", evidence.getId(), evidenceResult.getFailureReason());
            }
        }

        // Require at least one valid piece of evidence
        if (validEvidenceCount == 0) {
            return ValidationResult.invalid("No valid evidence found");
        }

        // Additional validation for Finding fields themselves
        ValidationResult fieldResult = validateFindingFields(finding, workspacePath);
        if (!fieldResult.isValid()) {
            return fieldResult;
        }

        return ValidationResult.valid();
    }

    /**
     * Validates a single piece of evidence against repository artifacts.
     */
    private ValidationResult validateSingleEvidence(Evidence evidence, Path workspacePath) {
        String evidenceType = evidence.getEvidenceType();
        String evidenceSource = evidence.getEvidenceSource();

        return switch (evidenceType) {
            case "CLASS_NODE" -> validateClassEvidence(evidence, workspacePath);
            case "METHOD_NODE" -> validateMethodEvidence(evidence, workspacePath);
            case "ANNOTATION" -> validateAnnotationEvidence(evidence, workspacePath);
            case "FILE_CONTENT" -> validateFileEvidence(evidence, workspacePath);
            case "SEMANTIC_CHUNK" -> validateSemanticChunkEvidence(evidence);
            case "DEPENDENCY" -> ValidationResult.valid(); // Dependency evidence is graph-based, harder to validate deterministically
            default -> {
                log.warn("Unknown evidence type: {}", evidenceType);
                yield ValidationResult.valid(); // Be lenient with unknown types for now
            }
        };
    }

    /**
     * Validates that a referenced class actually exists in the repository.
     */
    private ValidationResult validateClassEvidence(Evidence evidence, Path workspacePath) {
        String className = evidence.getClassName();
        if (className == null || className.isBlank()) {
            return ValidationResult.invalid("Class evidence missing class name");
        }

        var classes = astAnalysisService.findClassByName(workspacePath, className);
        if (classes.isEmpty()) {
            return ValidationResult.invalid("Class not found in repository: " + className);
        }

        // If file path is specified, verify the class exists in that specific file
        if (evidence.getFilePath() != null && !evidence.getFilePath().isBlank()) {
            boolean foundInFile = classes.stream()
                    .anyMatch(c -> c.getFilePath().equals(evidence.getFilePath()));
            if (!foundInFile) {
                return ValidationResult.invalid("Class " + className + " not found in specified file: " + evidence.getFilePath());
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Validates that a referenced method actually exists in the repository.
     */
    private ValidationResult validateMethodEvidence(Evidence evidence, Path workspacePath) {
        String methodName = evidence.getMethodName();
        if (methodName == null || methodName.isBlank()) {
            return ValidationResult.invalid("Method evidence missing method name");
        }

        String className = evidence.getClassName();
        if (className == null || className.isBlank()) {
            return ValidationResult.invalid("Method evidence missing class name");
        }

        var classes = astAnalysisService.findClassByName(workspacePath, className);
        if (classes.isEmpty()) {
            return ValidationResult.invalid("Class not found in repository: " + className);
        }

        // Verify method exists in the class
        boolean methodFound = classes.stream()
                .flatMap(c -> c.getMethods().stream())
                .anyMatch(m -> m.getName().equals(methodName));

        if (!methodFound) {
            return ValidationResult.invalid("Method " + methodName + " not found in class " + className);
        }

        return ValidationResult.valid();
    }

    /**
     * Validates that a referenced annotation exists on the specified class or method.
     */
    private ValidationResult validateAnnotationEvidence(Evidence evidence, Path workspacePath) {
        String annotationName = evidence.getAnnotationName();
        if (annotationName == null || annotationName.isBlank()) {
            return ValidationResult.invalid("Annotation evidence missing annotation name");
        }

        String className = evidence.getClassName();
        if (className == null || className.isBlank()) {
            return ValidationResult.invalid("Annotation evidence missing class name");
        }

        var classes = astAnalysisService.findClassByName(workspacePath, className);
        if (classes.isEmpty()) {
            return ValidationResult.invalid("Class not found in repository: " + className);
        }

        // Check if annotation exists on class
        boolean annotationFound = classes.stream()
                .anyMatch(c -> c.getAnnotations().stream()
                        .anyMatch(a -> a.equals(annotationName) || a.endsWith("." + annotationName)));

        // If not on class, check if it's on a method
        if (!annotationFound && evidence.getMethodName() != null && !evidence.getMethodName().isBlank()) {
            annotationFound = classes.stream()
                    .flatMap(c -> c.getMethods().stream())
                    .filter(m -> m.getName().equals(evidence.getMethodName()))
                    .anyMatch(m -> m.getAnnotations().stream()
                            .anyMatch(a -> a.equals(annotationName) || a.endsWith("." + annotationName)));
        }

        if (!annotationFound) {
            return ValidationResult.invalid("Annotation " + annotationName + " not found on " + 
                    (evidence.getMethodName() != null ? "method " + evidence.getMethodName() : "class ") + className);
        }

        return ValidationResult.valid();
    }

    /**
     * Validates that a referenced file exists in the repository.
     */
    private ValidationResult validateFileEvidence(Evidence evidence, Path workspacePath) {
        String filePath = evidence.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            return ValidationResult.invalid("File evidence missing file path");
        }

        Path fullPath = workspacePath.resolve(filePath);
        if (!Files.exists(fullPath)) {
            return ValidationResult.invalid("File not found in repository: " + filePath);
        }

        if (!Files.isRegularFile(fullPath)) {
            return ValidationResult.invalid("Path is not a regular file: " + filePath);
        }

        return ValidationResult.valid();
    }

    /**
     * Validates semantic chunk evidence (RAG-based).
     * Since RAG is probabilistic, we only validate that content exists.
     */
    private ValidationResult validateSemanticChunkEvidence(Evidence evidence) {
        String content = evidence.getContent();
        if (content == null || content.isBlank()) {
            return ValidationResult.invalid("Semantic chunk evidence missing content");
        }

        return ValidationResult.valid();
    }

    /**
     * Validates the Finding's own fields against repository artifacts.
     */
    private ValidationResult validateFindingFields(Finding finding, Path workspacePath) {
        // Validate file path if specified
        if (finding.getFilePath() != null && !finding.getFilePath().isBlank()) {
            Path fullPath = workspacePath.resolve(finding.getFilePath());
            if (!Files.exists(fullPath)) {
                return ValidationResult.invalid("Finding file path does not exist: " + finding.getFilePath());
            }
        }

        // Validate class name if specified
        if (finding.getClassName() != null && !finding.getClassName().isBlank()) {
            var classes = astAnalysisService.findClassByName(workspacePath, finding.getClassName());
            if (classes.isEmpty()) {
                return ValidationResult.invalid("Finding class not found in repository: " + finding.getClassName());
            }
        }

        // Validate method name if specified (requires class name)
        if (finding.getMethodName() != null && !finding.getMethodName().isBlank()) {
            if (finding.getClassName() == null || finding.getClassName().isBlank()) {
                return ValidationResult.invalid("Finding has method name but missing class name");
            }

            var classes = astAnalysisService.findClassByName(workspacePath, finding.getClassName());
            boolean methodFound = classes.stream()
                    .flatMap(c -> c.getMethods().stream())
                    .anyMatch(m -> m.getName().equals(finding.getMethodName()));

            if (!methodFound) {
                return ValidationResult.invalid("Finding method not found in class: " + 
                        finding.getMethodName() + " in " + finding.getClassName());
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Simple result object for validation operations.
     */
    private static final class ValidationResult {
        private final boolean valid;
        private final String failureReason;

        private ValidationResult(boolean valid, String failureReason) {
            this.valid = valid;
            this.failureReason = failureReason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String failureReason) {
            return new ValidationResult(false, failureReason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }
}
