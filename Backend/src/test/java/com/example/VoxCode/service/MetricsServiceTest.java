package com.example.VoxCode.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetricsService} (VXC-220 — Telemetry and Metrics).
 *
 * <p>Tests cover custom metrics collection for:
 * <ul>
 *   <li>Tool execution latency and counts</li>
 *   <li>LLM call latency and token usage</li>
 *   <li>Build and verification durations</li>
 *   <li>Remediation operation counts and durations</li>
 *   <li>Investigation lifecycle events</li>
 * </ul>
 */
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    // ─── Counter Tests ─────────────────────────────────────────────────────────

    @Test
    void testRecordToolExecution_incrementsCounter() {
        metricsService.recordToolExecution("AstTools", "success");

        double count = meterRegistry.counter("voxcode.tool.executions",
                "tool", "AstTools", "result", "success").count();

        assertEquals(1.0, count, 0.001);
    }

    @Test
    void testRecordLlmCall_incrementsCounter() {
        metricsService.recordLlmCall("ollama", "success");

        double count = meterRegistry.counter("voxcode.llm.calls",
                "model", "ollama", "result", "success").count();

        assertEquals(1.0, count, 0.001);
    }

    @Test
    void testRecordRemediation_incrementsCounter() {
        metricsService.recordRemediation("AST", "COMPLETED");

        double count = meterRegistry.counter("voxcode.remediation.operations",
                "strategy", "AST", "result", "COMPLETED").count();

        assertEquals(1.0, count, 0.001);
    }

    @Test
    void testRecordVerification_incrementsCounter() {
        metricsService.recordVerification("PASSED");

        double count = meterRegistry.counter("voxcode.verification.operations",
                "status", "PASSED").count();

        assertEquals(1.0, count, 0.001);
    }

    @Test
    void testRecordRollback_incrementsCounter() {
        metricsService.recordRollback("SCOPE_VIOLATION");

        double count = meterRegistry.counter("voxcode.rollback.operations",
                "trigger", "SCOPE_VIOLATION").count();

        assertEquals(1.0, count, 0.001);
    }

    @Test
    void testRecordInvestigationEvent_incrementsCounter() {
        metricsService.recordInvestigationEvent("started");

        double count = meterRegistry.counter("voxcode.investigation.events",
                "event", "started").count();

        assertEquals(1.0, count, 0.001);
    }

    // ─── Timer Tests ─────────────────────────────────────────────────────────────

    @Test
    void testRecordToolLatency_recordsTimer() {
        metricsService.recordToolLatency("AstTools", 100);

        assertTrue(meterRegistry.timer("voxcode.tool.latency", "tool", "AstTools").count() > 0);
    }

    @Test
    void testRecordLlmLatency_recordsTimer() {
        metricsService.recordLlmLatency("ollama", 500);

        assertTrue(meterRegistry.timer("voxcode.llm.latency", "model", "ollama").count() > 0);
    }

    @Test
    void testRecordBuildDuration_recordsTimer() {
        metricsService.recordBuildDuration("SUCCESS", 30000);

        assertTrue(meterRegistry.timer("voxcode.build.duration", "result", "SUCCESS").count() > 0);
    }

    @Test
    void testRecordVerificationDuration_recordsTimer() {
        metricsService.recordVerificationDuration("PASSED", 45000);

        assertTrue(meterRegistry.timer("voxcode.verification.duration", "status", "PASSED").count() > 0);
    }

    @Test
    void testRecordRemediationDuration_recordsTimer() {
        metricsService.recordRemediationDuration("AST", "COMPLETED", 15000);

        assertTrue(meterRegistry.timer("voxcode.remediation.duration",
                "strategy", "AST", "result", "COMPLETED").count() > 0);
    }

    @Test
    void testRecordInvestigationDuration_recordsTimer() {
        metricsService.recordInvestigationDuration("COMPLETED", 120000);

        assertTrue(meterRegistry.timer("voxcode.investigation.duration", "status", "COMPLETED").count() > 0);
    }

    // ─── Gauge Tests ─────────────────────────────────────────────────────────────

    @Test
    void testRecordLlmTokenUsage_recordsGauge() {
        metricsService.recordLlmTokenUsage("ollama", "input", 100);

        assertEquals(100.0, meterRegistry.find("voxcode.llm.tokens").gauge().value(), 0.001);
    }

    @Test
    void testRecordActiveInvestigations_recordsGauge() {
        metricsService.recordActiveInvestigations(5);

        assertEquals(5.0, meterRegistry.find("voxcode.investigation.active").gauge().value(), 0.001);
    }

    @Test
    void testRecordPendingApprovals_recordsGauge() {
        metricsService.recordPendingApprovals(3);

        assertEquals(3.0, meterRegistry.find("voxcode.approval.pending").gauge().value(), 0.001);
    }

    // ─── Multiple Recording Tests ───────────────────────────────────────────────

    @Test
    void testMultipleToolExecutions_accumulateCount() {
        metricsService.recordToolExecution("AstTools", "success");
        metricsService.recordToolExecution("AstTools", "success");
        metricsService.recordToolExecution("AstTools", "error");

        double successCount = meterRegistry.counter("voxcode.tool.executions",
                "tool", "AstTools", "result", "success").count();
        double errorCount = meterRegistry.counter("voxcode.tool.executions",
                "tool", "AstTools", "result", "error").count();

        assertEquals(2.0, successCount, 0.001);
        assertEquals(1.0, errorCount, 0.001);
    }

    @Test
    void testDifferentTools_trackedSeparately() {
        metricsService.recordToolExecution("AstTools", "success");
        metricsService.recordToolExecution("GraphTools", "success");

        double astCount = meterRegistry.counter("voxcode.tool.executions",
                "tool", "AstTools", "result", "success").count();
        double graphCount = meterRegistry.counter("voxcode.tool.executions",
                "tool", "GraphTools", "result", "success").count();

        assertEquals(1.0, astCount, 0.001);
        assertEquals(1.0, graphCount, 0.001);
    }
}
