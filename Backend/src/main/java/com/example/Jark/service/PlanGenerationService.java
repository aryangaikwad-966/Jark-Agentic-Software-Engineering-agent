package com.example.Jark.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Jark.entity.Evidence;
import com.example.Jark.entity.Finding;
import com.example.Jark.entity.Investigation;
import com.example.Jark.entity.Plan;
import com.example.Jark.repository.FindingRepository;
import com.example.Jark.repository.PlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates human-readable, actionable remediation plans from CONFIRMED findings.
 * Plans explicitly connect investigation to remediation with detailed scope boundaries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationService {

    private final PlanRepository planRepository;
    private final FindingRepository findingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates an engineering plan from a CONFIRMED finding.
     * Only CONFIRMED findings can be used for plan generation.
     *
     * @param findingId the ID of the CONFIRMED finding
     * @return the generated Plan
     * @throws IllegalArgumentException if the finding is not CONFIRMED
     */
    @Transactional
    public Plan generatePlanFromFinding(Long findingId) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found: " + findingId));

        if (!"CONFIRMED".equals(finding.getValidationStatus())) {
            throw new IllegalArgumentException(
                    "Only CONFIRMED findings can be used for plan generation. Current status: " + 
                    finding.getValidationStatus());
        }

        log.info("Generating engineering plan from CONFIRMED finding {}: {} ({})",
                findingId, finding.getIssueType(), finding.getSeverity());

        Investigation investigation = finding.getInvestigation();

        // Generate plan components
        String title = generatePlanTitle(finding);
        String description = generatePlanDescription(finding);
        String rootCause = analyzeRootCause(finding);
        List<String> affectedFiles = extractAffectedFiles(finding);
        List<String> affectedArtifacts = extractAffectedArtifacts(finding);
        String proposedChanges = generateProposedChanges(finding);
        String transformationStrategy = determineTransformationStrategy(finding);
        String risk = assessRisk(finding);
        String expectedBehavior = defineExpectedBehavior(finding);
        String verificationStrategy = defineVerificationStrategy(finding);
        String rollbackStrategy = defineRollbackStrategy(finding);
        Map<String, Object> scopeBoundaries = defineScopeBoundaries(finding);
        String estimatedImpact = estimateImpact(finding);

        // Create Plan entity
        Plan plan = new Plan();
        plan.setInvestigation(investigation);
        plan.setFinding(finding);
        plan.setTitle(title);
        plan.setDescription(description);
        plan.setRootCause(rootCause);

        try {
            plan.setAffectedFiles(objectMapper.writeValueAsString(affectedFiles));
            plan.setAffectedArtifacts(objectMapper.writeValueAsString(affectedArtifacts));
            plan.setProposedChanges(proposedChanges);
            plan.setScopeBoundaries(objectMapper.writeValueAsString(scopeBoundaries));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize plan JSON components", e);
            throw new IllegalStateException("Failed to serialize plan components", e);
        }

        plan.setTransformationStrategy(transformationStrategy);
        plan.setRisk(risk);
        plan.setExpectedBehavior(expectedBehavior);
        plan.setVerificationStrategy(verificationStrategy);
        plan.setRollbackStrategy(rollbackStrategy);
        plan.setEstimatedImpact(estimatedImpact);
        plan.setStatus("PENDING_APPROVAL");

        Plan savedPlan = planRepository.save(plan);

        log.info("Successfully generated Plan ID {} for Finding ID {}", savedPlan.getId(), findingId);

        return savedPlan;
    }

    /**
     * Generates a descriptive title for the plan.
     */
    private String generatePlanTitle(Finding finding) {
        return String.format("Remediation Plan: %s - %s (%s)",
                finding.getIssueType(),
                finding.getClassName() != null ? finding.getClassName() : finding.getFilePath(),
                finding.getSeverity());
    }

    /**
     * Generates a detailed description of the plan.
     */
    private String generatePlanDescription(Finding finding) {
        StringBuilder description = new StringBuilder();
        description.append("This plan addresses a ")
                .append(finding.getSeverity().toLowerCase())
                .append(" severity ")
                .append(finding.getIssueType())
                .append(" issue.\n\n");

        if (finding.getClassName() != null) {
            description.append("Affected class: ").append(finding.getClassName()).append("\n");
        }
        if (finding.getMethodName() != null) {
            description.append("Affected method: ").append(finding.getMethodName()).append("\n");
        }
        if (finding.getFilePath() != null) {
            description.append("Affected file: ").append(finding.getFilePath()).append("\n");
        }

        description.append("\nIssue description: ").append(finding.getDescription());

        return description.toString();
    }

    /**
     * Analyzes the root cause based on finding evidence.
     */
    private String analyzeRootCause(Finding finding) {
        List<Evidence> evidenceList = finding.getEvidence();
        if (evidenceList == null || evidenceList.isEmpty()) {
            return "Unable to determine root cause - no evidence available.";
        }

        StringBuilder rootCause = new StringBuilder();
        rootCause.append("Root cause analysis based on ").append(evidenceList.size()).append(" evidence items:\n");

        for (Evidence evidence : evidenceList) {
            rootCause.append("- ").append(evidence.getEvidenceSource())
                    .append(" evidence (").append(evidence.getEvidenceType()).append(")");
            if (evidence.getContent() != null && !evidence.getContent().isBlank()) {
                rootCause.append(": ").append(evidence.getContent().substring(0, Math.min(100, evidence.getContent().length())));
            }
            rootCause.append("\n");
        }

        return rootCause.toString();
    }

    /**
     * Extracts affected file paths from the finding and its evidence.
     */
    private List<String> extractAffectedFiles(Finding finding) {
        List<String> files = new ArrayList<>();

        // Add finding's file path
        if (finding.getFilePath() != null && !finding.getFilePath().isBlank()) {
            files.add(finding.getFilePath());
        }

        // Add file paths from evidence
        if (finding.getEvidence() != null) {
            finding.getEvidence().stream()
                    .filter(e -> e.getFilePath() != null && !e.getFilePath().isBlank())
                    .map(Evidence::getFilePath)
                    .filter(filePath -> !files.contains(filePath))
                    .forEach(files::add);
        }

        return files;
    }

    /**
     * Extracts affected classes and methods from the finding and its evidence.
     */
    private List<String> extractAffectedArtifacts(Finding finding) {
        List<String> artifacts = new ArrayList<>();

        // Add finding's class and method
        if (finding.getClassName() != null && !finding.getClassName().isBlank()) {
            artifacts.add("Class: " + finding.getClassName());
        }
        if (finding.getMethodName() != null && !finding.getMethodName().isBlank()) {
            artifacts.add("Method: " + finding.getMethodName());
        }

        // Add classes and methods from evidence
        if (finding.getEvidence() != null) {
            finding.getEvidence().stream()
                    .filter(e -> e.getClassName() != null && !e.getClassName().isBlank())
                    .forEach(e -> {
                        String artifact = "Class: " + e.getClassName();
                        if (e.getMethodName() != null && !e.getMethodName().isBlank()) {
                            artifact += ", Method: " + e.getMethodName();
                        }
                        if (!artifacts.contains(artifact)) {
                            artifacts.add(artifact);
                        }
                    });
        }

        return artifacts;
    }

    /**
     * Generates detailed proposed changes based on the finding type.
     */
    private String generateProposedChanges(Finding finding) {
        Map<String, Object> changes = new HashMap<>();

        // Basic change information
        changes.put("issueType", finding.getIssueType());
        changes.put("severity", finding.getSeverity());
        changes.put("description", finding.getDescription());

        // Specific changes based on issue type
        Map<String, Object> specificChanges = new HashMap<>();
        switch (finding.getIssueType()) {
            case "SECURITY":
                specificChanges.put("changeType", "Security fix");
                specificChanges.put("requires", "Add missing security controls");
                break;
            case "VALIDATION":
                specificChanges.put("changeType", "Input validation");
                specificChanges.put("requires", "Add validation logic");
                break;
            case "ARCHITECTURE":
                specificChanges.put("changeType", "Architectural improvement");
                specificChanges.put("requires", "Refactor structure");
                break;
            case "ERROR_HANDLING":
                specificChanges.put("changeType", "Error handling improvement");
                specificChanges.put("requires", "Add proper exception handling");
                break;
            default:
                specificChanges.put("changeType", "General fix");
                specificChanges.put("requires", "Address identified issue");
        }

        changes.put("specificChanges", specificChanges);

        // Location information
        if (finding.getFilePath() != null) {
            changes.put("file", finding.getFilePath());
        }
        if (finding.getClassName() != null) {
            changes.put("class", finding.getClassName());
        }
        if (finding.getMethodName() != null) {
            changes.put("method", finding.getMethodName());
        }
        if (finding.getLineRange() != null) {
            changes.put("lineRange", finding.getLineRange());
        }

        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize proposed changes", e);
            return "{}";
        }
    }

    /**
     * Determines the transformation strategy based on the finding.
     */
    private String determineTransformationStrategy(Finding finding) {
        String severity = finding.getSeverity();
        String issueType = finding.getIssueType();

        StringBuilder strategy = new StringBuilder();
        strategy.append("Transformation strategy for ").append(severity).append(" ").append(issueType).append(":\n");

        switch (severity) {
            case "CRITICAL":
                strategy.append("- Immediate remediation required\n");
                strategy.append("- Deploy as hotfix if necessary\n");
                strategy.append("- Coordinate with security team for SECURITY issues\n");
                break;
            case "HIGH":
                strategy.append("- Priority remediation in next sprint\n");
                strategy.append("- Consider backporting to stable branches\n");
                break;
            case "MEDIUM":
                strategy.append("- Include in upcoming release\n");
                strategy.append("- Normal development process\n");
                break;
            case "LOW":
                strategy.append("- Include in backlog prioritization\n");
                strategy.append("- Can be deferred if resource-constrained\n");
                break;
            case "INFO":
                strategy.append("- Informational only, no immediate action required\n");
                strategy.append("- Consider for future improvements\n");
                break;
        }

        strategy.append("- Code review required\n");
        strategy.append("- Unit tests must be updated/added\n");
        strategy.append("- Integration tests must pass\n");

        return strategy.toString();
    }

    /**
     * Assesses the risk of the proposed changes.
     */
    private String assessRisk(Finding finding) {
        String severity = finding.getSeverity();
        String issueType = finding.getIssueType();

        StringBuilder risk = new StringBuilder();
        risk.append("Risk assessment:\n");

        switch (severity) {
            case "CRITICAL":
                risk.append("- HIGH RISK: Critical issue requiring immediate attention\n");
                risk.append("- Potential security vulnerability or data loss\n");
                risk.append("- May require coordination with multiple teams\n");
                break;
            case "HIGH":
                risk.append("- MODERATE to HIGH RISK: Significant issue\n");
                risk.append("- May impact system stability or performance\n");
                risk.append("- Requires thorough testing\n");
                break;
            case "MEDIUM":
                risk.append("- MODERATE RISK: Standard issue\n");
                risk.append("- Normal testing requirements\n");
                risk.append("- Standard review process\n");
                break;
            case "LOW":
                risk.append("- LOW RISK: Minor issue\n");
                risk.append("- Minimal impact expected\n");
                risk.append("- Standard testing sufficient\n");
                break;
            case "INFO":
                risk.append("- VERY LOW RISK: Informational\n");
                risk.append("- No functional impact expected\n");
                break;
        }

        if ("SECURITY".equals(issueType)) {
            risk.append("- SECURITY CONSIDERATIONS: Follow security review process\n");
            risk.append("- Consider security implications of changes\n");
        }

        return risk.toString();
    }

    /**
     * Defines the expected behavior after changes are applied.
     */
    private String defineExpectedBehavior(Finding finding) {
        StringBuilder behavior = new StringBuilder();
        behavior.append("Expected behavior after remediation:\n");

        switch (finding.getIssueType()) {
            case "SECURITY":
                behavior.append("- Security controls properly in place\n");
                behavior.append("- Unauthorized access prevented\n");
                behavior.append("- Security scans pass\n");
                break;
            case "VALIDATION":
                behavior.append("- Input properly validated\n");
                behavior.append("- Invalid inputs rejected appropriately\n");
                behavior.append("- Error messages are user-friendly\n");
                break;
            case "ARCHITECTURE":
                behavior.append("- Code follows architectural patterns\n");
                behavior.append("- Dependencies properly structured\n");
                behavior.append("- No circular dependencies\n");
                break;
            case "ERROR_HANDLING":
                behavior.append("- Exceptions properly caught and handled\n");
                behavior.append("- Error messages are informative\n");
                behavior.append("- System recovers gracefully from errors\n");
                break;
            default:
                behavior.append("- Identified issue resolved\n");
                behavior.append("- System functions as expected\n");
        }

        behavior.append("- All existing tests pass\n");
        behavior.append("- New tests for the fix pass\n");

        return behavior.toString();
    }

    /**
     * Defines the verification strategy for the changes.
     */
    private String defineVerificationStrategy(Finding finding) {
        StringBuilder verification = new StringBuilder();
        verification.append("Verification strategy:\n");

        verification.append("- Unit tests: Add/update tests for the affected code\n");
        verification.append("- Integration tests: Verify system behavior end-to-end\n");
        verification.append("- Static analysis: Run code quality checks\n");

        if ("SECURITY".equals(finding.getIssueType())) {
            verification.append("- Security scanning: Run security analysis tools\n");
            verification.append("- Penetration testing: If applicable\n");
        }

        verification.append("- Manual testing: Test the specific functionality\n");
        verification.append("- Regression testing: Ensure no unintended side effects\n");

        return verification.toString();
    }

    /**
     * Defines the rollback strategy if issues occur.
     */
    private String defineRollbackStrategy(Finding finding) {
        StringBuilder rollback = new StringBuilder();
        rollback.append("Rollback strategy:\n");

        rollback.append("- Maintain backup of original code\n");
        rollback.append("- Use version control to revert changes if needed\n");
        rollback.append("- Document exact changes made\n");

        if ("CRITICAL".equals(finding.getSeverity())) {
            rollback.append("- Prepare hotfix rollback procedure\n");
            rollback.append("- Test rollback procedure in staging\n");
        }

        rollback.append("- Monitor system after deployment\n");
        rollback.append("- Quick rollback if issues detected\n");

        return rollback.toString();
    }

    /**
     * Defines scope boundaries for what is allowed to change.
     */
    private Map<String, Object> defineScopeBoundaries(Finding finding) {
        Map<String, Object> boundaries = new HashMap<>();

        // Files that can be modified
        List<String> allowedFiles = extractAffectedFiles(finding);
        boundaries.put("allowedFiles", allowedFiles);

        // Classes that can be modified
        List<String> allowedClasses = new ArrayList<>();
        if (finding.getClassName() != null) {
            allowedClasses.add(finding.getClassName());
        }
        if (finding.getEvidence() != null) {
            finding.getEvidence().stream()
                    .filter(e -> e.getClassName() != null && !e.getClassName().isBlank())
                    .map(Evidence::getClassName)
                    .filter(className -> !allowedClasses.contains(className))
                    .forEach(allowedClasses::add);
        }
        boundaries.put("allowedClasses", allowedClasses);

        // Methods that can be modified
        List<String> allowedMethods = new ArrayList<>();
        if (finding.getMethodName() != null) {
            allowedMethods.add(finding.getMethodName());
        }
        if (finding.getEvidence() != null) {
            finding.getEvidence().stream()
                    .filter(e -> e.getMethodName() != null && !e.getMethodName().isBlank())
                    .map(Evidence::getMethodName)
                    .filter(methodName -> !allowedMethods.contains(methodName))
                    .forEach(allowedMethods::add);
        }
        boundaries.put("allowedMethods", allowedMethods);

        // Scope restrictions
        boundaries.put("restrictions", List.of(
                "Only modify identified files, classes, and methods",
                "Do not modify unrelated code",
                "Follow existing code style and patterns",
                "Maintain backward compatibility where possible"
        ));

        return boundaries;
    }

    /**
     * Estimates the impact of the proposed changes.
     */
    private String estimateImpact(Finding finding) {
        StringBuilder impact = new StringBuilder();
        impact.append("Impact estimation:\n");

        int affectedFileCount = extractAffectedFiles(finding).size();
        int affectedArtifactCount = extractAffectedArtifacts(finding).size();

        impact.append("- Affected files: ").append(affectedFileCount).append("\n");
        impact.append("- Affected artifacts: ").append(affectedArtifactCount).append("\n");

        switch (finding.getSeverity()) {
            case "CRITICAL":
                impact.append("- Impact: HIGH - System-wide impact possible\n");
                impact.append("- Downtime may be required\n");
                break;
            case "HIGH":
                impact.append("- Impact: MODERATE to HIGH - Significant system impact\n");
                impact.append("- Some disruption possible\n");
                break;
            case "MEDIUM":
                impact.append("- Impact: MODERATE - Limited system impact\n");
                impact.append("- Minimal disruption expected\n");
                break;
            case "LOW":
                impact.append("- Impact: LOW - Minimal system impact\n");
                impact.append("- No disruption expected\n");
                break;
            case "INFO":
                impact.append("- Impact: NONE - No functional impact\n");
                break;
        }

        impact.append("- Testing effort: ");
        if (affectedFileCount > 5) {
            impact.append("HIGH - Comprehensive testing required\n");
        } else if (affectedFileCount > 2) {
            impact.append("MODERATE - Standard testing required\n");
        } else {
            impact.append("LOW - Basic testing sufficient\n");
        }

        return impact.toString();
    }

    /**
     * Generates plans for all CONFIRMED findings in an investigation.
     *
     * @param investigationId the investigation ID
     * @return list of generated plans
     */
    @Transactional
    public List<Plan> generatePlansForInvestigation(Long investigationId) {
        List<Finding> confirmedFindings = findingRepository.findByInvestigationId(investigationId).stream()
                .filter(f -> "CONFIRMED".equals(f.getValidationStatus()))
                .collect(Collectors.toList());

        log.info("Generating plans for {} CONFIRMED findings in investigation {}",
                confirmedFindings.size(), investigationId);

        return confirmedFindings.stream()
                .map(finding -> generatePlanFromFinding(finding.getId()))
                .collect(Collectors.toList());
    }
}
