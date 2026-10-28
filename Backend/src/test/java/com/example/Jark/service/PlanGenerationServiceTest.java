package com.example.Jark.service;

import com.example.Jark.entity.CodeRepository;
import com.example.Jark.entity.Evidence;
import com.example.Jark.entity.Finding;
import com.example.Jark.entity.Investigation;
import com.example.Jark.entity.Plan;
import com.example.Jark.repository.FindingRepository;
import com.example.Jark.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanGenerationServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FindingRepository findingRepository;

    @InjectMocks
    private PlanGenerationService planGenerationService;

    private Finding confirmedFinding;
    private Investigation investigation;
    private CodeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CodeRepository();
        repository.setId(1L);
        repository.setName("test-repo");
        repository.setLocalPath("/tmp/test-workspace");

        investigation = new Investigation();
        investigation.setId(1L);
        investigation.setRepository(repository);

        confirmedFinding = new Finding();
        confirmedFinding.setId(1L);
        confirmedFinding.setInvestigation(investigation);
        confirmedFinding.setRepository(repository);
        confirmedFinding.setFilePath("src/main/java/com/example/Test.java");
        confirmedFinding.setClassName("Test");
        confirmedFinding.setMethodName("testMethod");
        confirmedFinding.setIssueType("SECURITY");
        confirmedFinding.setSeverity("HIGH");
        confirmedFinding.setDescription("Missing security annotation");
        confirmedFinding.setValidationStatus("CONFIRMED");
        confirmedFinding.setLineRange("10-20");
        confirmedFinding.setEvidence(new ArrayList<>());

        // Add some evidence
        Evidence evidence1 = new Evidence();
        evidence1.setEvidenceSource("AST");
        evidence1.setEvidenceType("CLASS_NODE");
        evidence1.setClassName("Test");
        evidence1.setFilePath("src/main/java/com/example/Test.java");
        evidence1.setContent("Class node content");
        confirmedFinding.getEvidence().add(evidence1);

        Evidence evidence2 = new Evidence();
        evidence2.setEvidenceSource("AST");
        evidence2.setEvidenceType("METHOD_NODE");
        evidence2.setClassName("Test");
        evidence2.setMethodName("testMethod");
        evidence2.setContent("Method node content");
        confirmedFinding.getEvidence().add(evidence2);
    }

    @Test
    void testGeneratePlanFromFinding_Success() {
        when(findingRepository.findById(1L)).thenReturn(Optional.of(confirmedFinding));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        Plan result = planGenerationService.generatePlanFromFinding(1L);

        // Verify
        assertNotNull(result);
        assertEquals(investigation, result.getInvestigation());
        assertEquals(confirmedFinding, result.getFinding());
        assertNotNull(result.getTitle());
        assertTrue(result.getTitle().contains("SECURITY"));
        assertTrue(result.getTitle().contains("HIGH"));
        assertNotNull(result.getDescription());
        assertNotNull(result.getRootCause());
        assertNotNull(result.getAffectedFiles());
        assertNotNull(result.getAffectedArtifacts());
        assertNotNull(result.getProposedChanges());
        assertNotNull(result.getTransformationStrategy());
        assertNotNull(result.getRisk());
        assertNotNull(result.getExpectedBehavior());
        assertNotNull(result.getVerificationStrategy());
        assertNotNull(result.getRollbackStrategy());
        assertNotNull(result.getScopeBoundaries());
        assertNotNull(result.getEstimatedImpact());
        assertEquals("PENDING_APPROVAL", result.getStatus());

        verify(findingRepository).findById(1L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void testGeneratePlanFromFinding_NotConfirmed() {
        confirmedFinding.setValidationStatus("PENDING_VALIDATION");

        when(findingRepository.findById(1L)).thenReturn(Optional.of(confirmedFinding));

        // Execute & Verify
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> planGenerationService.generatePlanFromFinding(1L)
        );

        assertTrue(exception.getMessage().contains("Only CONFIRMED findings"));
        assertTrue(exception.getMessage().contains("PENDING_VALIDATION"));

        verify(findingRepository).findById(1L);
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void testGeneratePlanFromFinding_FindingNotFound() {
        when(findingRepository.findById(1L)).thenReturn(Optional.empty());

        // Execute & Verify
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> planGenerationService.generatePlanFromFinding(1L)
        );

        assertTrue(exception.getMessage().contains("Finding not found"));

        verify(findingRepository).findById(1L);
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void testGeneratePlanFromFinding_ValidationIssue() {
        confirmedFinding.setValidationStatus("REJECTED");

        when(findingRepository.findById(1L)).thenReturn(Optional.of(confirmedFinding));

        // Execute & Verify
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> planGenerationService.generatePlanFromFinding(1L)
        );

        assertTrue(exception.getMessage().contains("Only CONFIRMED findings"));
        assertTrue(exception.getMessage().contains("REJECTED"));

        verify(findingRepository).findById(1L);
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void testGeneratePlansForInvestigation() {
        // Create multiple findings
        Finding finding1 = createMockFinding(1L, "SECURITY", "HIGH", "CONFIRMED");
        Finding finding2 = createMockFinding(2L, "VALIDATION", "MEDIUM", "CONFIRMED");
        Finding finding3 = createMockFinding(3L, "ARCHITECTURE", "LOW", "PENDING_VALIDATION"); // Should be skipped

        when(findingRepository.findByInvestigationId(1L))
                .thenReturn(List.of(finding1, finding2, finding3));
        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding1));
        when(findingRepository.findById(2L)).thenReturn(Optional.of(finding2));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        List<Plan> result = planGenerationService.generatePlansForInvestigation(1L);

        // Verify
        assertEquals(2, result.size()); // Only CONFIRMED findings
        verify(findingRepository).findByInvestigationId(1L);
        verify(findingRepository).findById(1L);
        verify(findingRepository).findById(2L);
        verify(findingRepository, never()).findById(3L); // PENDING finding should be skipped
        verify(planRepository, times(2)).save(any(Plan.class));
    }

    @Test
    void testGeneratePlansForInvestigation_NoConfirmedFindings() {
        Finding finding1 = createMockFinding(1L, "SECURITY", "HIGH", "PENDING_VALIDATION");
        Finding finding2 = createMockFinding(2L, "VALIDATION", "MEDIUM", "REJECTED");

        when(findingRepository.findByInvestigationId(1L))
                .thenReturn(List.of(finding1, finding2));

        // Execute
        List<Plan> result = planGenerationService.generatePlansForInvestigation(1L);

        // Verify
        assertEquals(0, result.size());
        verify(findingRepository).findByInvestigationId(1L);
        verify(findingRepository, never()).findById(any());
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void testGeneratePlanFromFinding_NoEvidence() {
        confirmedFinding.setEvidence(new ArrayList<>());

        when(findingRepository.findById(1L)).thenReturn(Optional.of(confirmedFinding));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        Plan result = planGenerationService.generatePlanFromFinding(1L);

        // Verify
        assertNotNull(result);
        assertTrue(result.getRootCause().contains("Unable to determine root cause"));

        verify(findingRepository).findById(1L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void testGeneratePlanFromFinding_NoClassOrMethod() {
        confirmedFinding.setClassName(null);
        confirmedFinding.setMethodName(null);

        when(findingRepository.findById(1L)).thenReturn(Optional.of(confirmedFinding));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        Plan result = planGenerationService.generatePlanFromFinding(1L);

        // Verify
        assertNotNull(result);
        assertTrue(result.getDescription().contains(confirmedFinding.getFilePath()));

        verify(findingRepository).findById(1L);
        verify(planRepository).save(any(Plan.class));
    }

    // Helper method to create mock findings
    private Finding createMockFinding(Long id, String issueType, String severity, String validationStatus) {
        Finding finding = new Finding();
        finding.setId(id);
        finding.setInvestigation(investigation);
        finding.setRepository(repository);
        finding.setFilePath("src/main/java/com/example/Test" + id + ".java");
        finding.setClassName("Test" + id);
        finding.setMethodName("testMethod" + id);
        finding.setIssueType(issueType);
        finding.setSeverity(severity);
        finding.setDescription("Test finding " + id);
        finding.setValidationStatus(validationStatus);
        finding.setLineRange("10-20");
        finding.setEvidence(new ArrayList<>());

        Evidence evidence = new Evidence();
        evidence.setEvidenceSource("AST");
        evidence.setEvidenceType("CLASS_NODE");
        evidence.setClassName("Test" + id);
        evidence.setContent("Class node content");
        finding.getEvidence().add(evidence);

        return finding;
    }
}
