package com.example.Jark.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for collecting and reporting custom metrics (VXC-220 — Telemetry and Metrics).
 *
 * <p>Provides metrics for:
 * <ul>
 *   <li>Tool execution latency</li>
 *   <li>LLM call latency and token usage</li>
 *   <li>Build and verification durations</li>
 *   <li>Remediation operation counts</li>
 *   <li>Investigation lifecycle events</li>
 * </ul>
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicInteger> llmTokenUsage = new ConcurrentHashMap<>();
    private final AtomicInteger activeInvestigations = new AtomicInteger();
    private final AtomicInteger pendingApprovals = new AtomicInteger();

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ─── Counters ────────────────────────────────────────────────────────────────

    /**
     * Records a tool execution.
     */
    public void recordToolExecution(String toolName, String result) {
        Counter.builder("jark.tool.executions")
                .tag("tool", toolName)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
        log.debug("Recorded tool execution: tool={}, result={}", toolName, result);
    }

    /**
     * Records an LLM call.
     */
    public void recordLlmCall(String model, String result) {
        Counter.builder("jark.llm.calls")
                .tag("model", model)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
        log.debug("Recorded LLM call: model={}, result={}", model, result);
    }

    /**
     * Records a remediation operation.
     */
    public void recordRemediation(String strategy, String result) {
        Counter.builder("jark.remediation.operations")
                .tag("strategy", strategy)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
        log.debug("Recorded remediation: strategy={}, result={}", strategy, result);
    }

    /**
     * Records a verification operation.
     */
    public void recordVerification(String status) {
        Counter.builder("jark.verification.operations")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
        log.debug("Recorded verification: status={}", status);
    }

    /**
     * Records a rollback operation.
     */
    public void recordRollback(String trigger) {
        Counter.builder("jark.rollback.operations")
                .tag("trigger", trigger)
                .register(meterRegistry)
                .increment();
        log.debug("Recorded rollback: trigger={}", trigger);
    }

    /**
     * Records an investigation lifecycle event.
     */
    public void recordInvestigationEvent(String event) {
        Counter.builder("jark.investigation.events")
                .tag("event", event)
                .register(meterRegistry)
                .increment();
        log.debug("Recorded investigation event: event={}", event);
    }

    // ─── Timers ─────────────────────────────────────────────────────────────────

    /**
     * Records tool execution latency.
     */
    public void recordToolLatency(String toolName, long durationMs) {
        Timer.builder("jark.tool.latency")
                .tag("tool", toolName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Recorded tool latency: tool={}, duration={}ms", toolName, durationMs);
    }

    /**
     * Records LLM call latency.
     */
    public void recordLlmLatency(String model, long durationMs) {
        Timer.builder("jark.llm.latency")
                .tag("model", model)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Recorded LLM latency: model={}, duration={}ms", model, durationMs);
    }

    /**
     * Records build duration.
     */
    public void recordBuildDuration(String result, long durationMs) {
        Timer.builder("jark.build.duration")
                .tag("result", result)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Recorded build duration: result={}, duration={}ms", result, durationMs);
    }

    /**
     * Records verification duration.
     */
    public void recordVerificationDuration(String status, long durationMs) {
        Timer.builder("jark.verification.duration")
                .tag("status", status)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Recorded verification duration: status={}, duration={}ms", status, durationMs);
    }

    /**
     * Records remediation duration.
     */
    public void recordRemediationDuration(String strategy, String result, long durationMs) {
        Timer.builder("jark.remediation.duration")
                .tag("strategy", strategy)
                .tag("result", result)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Recorded remediation duration: strategy={}, result={}, duration={}ms", strategy, result, durationMs);
    }

    /**
     * Records investigation duration.
     */
    public void recordInvestigationDuration(String status, long durationMs) {
        Timer.builder("jark.investigation.duration")
                .tag("status", status)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Recorded investigation duration: status={}, duration={}ms", status, durationMs);
    }

    // ─── Gauges ─────────────────────────────────────────────────────────────────

    /**
     * Records LLM token usage.
     */
    public void recordLlmTokenUsage(String model, String type, int tokens) {
        String key = model + '\u0000' + type;
        AtomicInteger usage = llmTokenUsage.computeIfAbsent(key, ignored -> {
            AtomicInteger gauge = new AtomicInteger();
            meterRegistry.gauge("jark.llm.tokens", Tags.of("model", model, "type", type), gauge);
            return gauge;
        });
        usage.set(Math.max(0, tokens));
        log.debug("Recorded LLM token usage: model={}, type={}, tokens={}", model, type, tokens);
    }

    /**
     * Records active investigations count.
     */
    public void recordActiveInvestigations(int count) {
        meterRegistry.gauge("jark.investigation.active", activeInvestigations);
        activeInvestigations.set(Math.max(0, count));
        log.debug("Recorded active investigations: count={}", count);
    }

    /**
     * Records pending approvals count.
     */
    public void recordPendingApprovals(int count) {
        meterRegistry.gauge("jark.approval.pending", pendingApprovals);
        pendingApprovals.set(Math.max(0, count));
        log.debug("Recorded pending approvals: count={}", count);
    }
}
