package com.example.VoxCode.evidence.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.VoxCode.dto.ast.ClassInfo;
import com.example.VoxCode.dto.ast.MethodInfo;
import com.example.VoxCode.dto.index.GraphQueryResult;
import com.example.VoxCode.dto.rag.CodeChunk;
import com.example.VoxCode.entity.Evidence;
import com.example.VoxCode.entity.Finding;
import com.example.VoxCode.entity.Investigation;
import com.example.VoxCode.evidence.model.EvidenceItem;
import com.example.VoxCode.evidence.model.LineRange;
import com.example.VoxCode.evidence.model.StructuredFinding;
import com.example.VoxCode.repository.EvidenceRepository;
import com.example.VoxCode.repository.FindingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Evidence Engine service that bridges LLM agent findings with deterministic
 * repository artifacts (AST nodes, Graph dependencies, RAG contexts, and file contents).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceEngine {

    private final FindingRepository findingRepository;
    private final EvidenceRepository evidenceRepository;
    private final FindingSchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;

    /**
     * Creates and persists a formal Finding and all its associated Evidence records
     * after validating against the strict schema.
     */
    @Transactional
    public Finding recordFinding(Investigation investigation, StructuredFinding structuredFinding) {
        log.info("Recording finding for investigation {}: {} ({})",
                investigation.getId(), structuredFinding.title(), structuredFinding.issueType());

        // 1. Strict schema validation
        schemaValidator.validate(structuredFinding).throwIfInvalid();

        // 2. Build Finding entity
        Finding finding = new Finding();
        finding.setInvestigation(investigation);
        finding.setRepository(investigation.getRepository());
        finding.setFilePath(structuredFinding.filePath());
        finding.setLineRange(structuredFinding.lineRange() != null 
                ? structuredFinding.lineRange().startLine() + "-" + structuredFinding.lineRange().endLine() 
                : null);
        finding.setClassName(structuredFinding.targetClass());
        finding.setMethodName(structuredFinding.targetMethod());
        finding.setIssueType(structuredFinding.issueType());
        finding.setSeverity(structuredFinding.severity());
        finding.setDescription(structuredFinding.description() != null ? structuredFinding.description() : "");
        finding.setValidationStatus(structuredFinding.validationStatus());
        finding.setExplanation(structuredFinding.description()); // Use description as explanation for now

        try {
            // Complete structured finding schema JSON stored in metadata
            finding.setMetadata(objectMapper.writeValueAsString(structuredFinding));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize finding JSON metadata", e);
        }

        Finding savedFinding = findingRepository.save(finding);

        // 3. Build and persist child Evidence records
        saveEvidenceReferences(savedFinding, structuredFinding);

        log.info("Successfully recorded Finding ID {} with {} evidence items",
                savedFinding.getId(),
                structuredFinding.evidenceReferences() != null ? structuredFinding.evidenceReferences().size() : 0);

        return savedFinding;
    }

    private void saveEvidenceReferences(Finding savedFinding, StructuredFinding structuredFinding) {
        if (structuredFinding.evidenceReferences() == null) {
            return;
        }
        for (EvidenceItem item : structuredFinding.evidenceReferences()) {
            Evidence evidence = new Evidence();
            evidence.setFinding(savedFinding);
            evidence.setEvidenceSource(item.source());
            evidence.setEvidenceType(item.evidenceType());
            evidence.setContent(item.content());
            evidence.setConfidence(item.confidenceScore() != null ? item.confidenceScore() : 1.0);

            populateMetadataFields(evidence, item);
            setEvidenceMetadataJson(evidence, item);

            evidenceRepository.save(evidence);
        }
    }

    private void populateMetadataFields(Evidence evidence, EvidenceItem item) {
        if (item.metadata() == null) {
            return;
        }
        if (item.metadata().containsKey("filePath")) {
            evidence.setFilePath((String) item.metadata().get("filePath"));
        }
        if (item.metadata().containsKey("className")) {
            evidence.setClassName((String) item.metadata().get("className"));
        }
        if (item.metadata().containsKey("methodName")) {
            evidence.setMethodName((String) item.metadata().get("methodName"));
        }
        if (item.metadata().containsKey("annotationName")) {
            evidence.setAnnotationName((String) item.metadata().get("annotationName"));
        }
        if (item.metadata().containsKey("startLine") && item.metadata().containsKey("endLine")) {
            int startLine = ((Number) item.metadata().get("startLine")).intValue();
            int endLine = ((Number) item.metadata().get("endLine")).intValue();
            evidence.setLineRange(startLine + "-" + endLine);
        }
    }

    private void setEvidenceMetadataJson(Evidence evidence, EvidenceItem item) {
        try {
            evidence.setMetadata(item.metadata() != null
                    ? objectMapper.writeValueAsString(item.metadata())
                    : "{}");
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize evidence item metadata, storing empty JSON", e);
            evidence.setMetadata("{}");
        }
    }

    /**
     * Converts an AST ClassInfo into a formal deterministic EvidenceItem.
     */
    public EvidenceItem createAstClassEvidence(String source, ClassInfo classInfo, String content) {
        LineRange lineRange = (classInfo.getStartLine() > 0 && classInfo.getEndLine() >= classInfo.getStartLine())
                ? LineRange.of(classInfo.getStartLine(), classInfo.getEndLine())
                : null;

        Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("fullyQualifiedName", classInfo.getFullyQualifiedName());
        meta.put("className", classInfo.getClassName());
        if (classInfo.getAnnotations() != null) {
            meta.put("annotations", classInfo.getAnnotations());
        }
        if (classInfo.getFilePath() != null) {
            meta.put("filePath", classInfo.getFilePath());
        }

        return EvidenceItem.astNode(source, content, "CLASS", lineRange, meta);
    }

    /**
     * Converts an AST MethodInfo into a formal deterministic EvidenceItem.
     */
    public EvidenceItem createAstMethodEvidence(String source, MethodInfo methodInfo, String content) {
        LineRange lineRange = (methodInfo.getStartLine() > 0 && methodInfo.getEndLine() >= methodInfo.getStartLine())
                ? LineRange.of(methodInfo.getStartLine(), methodInfo.getEndLine())
                : null;

        Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("methodName", methodInfo.getName());
        meta.put("returnType", methodInfo.getReturnType());
        if (methodInfo.getAnnotations() != null) {
            meta.put("annotations", methodInfo.getAnnotations());
        }
        if (methodInfo.getParameterTypes() != null) {
            meta.put("parameterTypes", methodInfo.getParameterTypes());
        }

        return EvidenceItem.astNode(source, content, "METHOD", lineRange, meta);
    }

    /**
     * Converts a Dependency Graph query result into a formal deterministic EvidenceItem.
     */
    public EvidenceItem createGraphEvidence(String source, GraphQueryResult graphResult, String fromClass, String toClass) {
        String content = "Graph relationship: " + fromClass + " -> " + toClass + " (nodes: " +
                (graphResult.getNodes() != null ? graphResult.getNodes().size() : 0) + ", relationships: " +
                (graphResult.getRelationships() != null ? graphResult.getRelationships().size() : 0) + ")";

        return EvidenceItem.graphEdge(source, content, "DEPENDS_ON", fromClass, toClass);
    }

    /**
     * Converts a RAG code chunk into a formal deterministic EvidenceItem.
     */
    public EvidenceItem createRagEvidence(String source, CodeChunk chunk, double similarityScore) {
        Map<String, Object> meta = new java.util.HashMap<>();
        if (chunk.getFilePath() != null) {
            meta.put("filePath", chunk.getFilePath());
        }
        if (chunk.getStartLine() > 0 && chunk.getEndLine() >= chunk.getStartLine()) {
            meta.put("startLine", chunk.getStartLine());
            meta.put("endLine", chunk.getEndLine());
        }

        return EvidenceItem.ragChunk(source, chunk.getContent() != null ? chunk.getContent() : "", similarityScore, meta);
    }

    /**
     * Retrieves all findings for a given investigation.
     */
    @Transactional(readOnly = true)
    public List<Finding> getFindingsForInvestigation(Long investigationId) {
        return findingRepository.findByInvestigationId(investigationId);
    }

    /**
     * Retrieves all evidence items attached to a finding.
     */
    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceForFinding(Long findingId) {
        return evidenceRepository.findByFindingId(findingId);
    }

    /**
     * Deserializes the full StructuredFinding schema from a persisted Finding.
     */
    @Transactional(readOnly = true)
    public Optional<StructuredFinding> getStructuredFinding(Long findingId) {
        return findingRepository.findById(findingId).flatMap(finding -> {
            if (finding.getMetadata() == null || finding.getMetadata().isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(objectMapper.readValue(finding.getMetadata(), StructuredFinding.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse metadata for finding {}", findingId, e);
                return Optional.empty();
            }
        });
    }
}
