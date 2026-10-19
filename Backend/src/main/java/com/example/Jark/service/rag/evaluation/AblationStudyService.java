package com.example.Jark.service.rag.evaluation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.Jark.service.rag.RagService;

/**
 * Runs the same benchmark against a baseline and the full RAG pipeline.
 */
@Service
public class AblationStudyService {

    public AblationStudyReport compare(
            RagService baseline,
            RagService fullRag,
            List<RetrievalBenchmarkCase> benchmarkCases,
            int k) {
        RetrievalEvaluationReport baselineReport = new RetrievalEvaluationService(baseline)
                .evaluateReport(benchmarkCases, k);
        RetrievalEvaluationReport fullReport = new RetrievalEvaluationService(fullRag)
                .evaluateReport(benchmarkCases, k);

        return new AblationStudyReport(
                baselineReport,
                fullReport,
                fullReport.meanRecallAtK() - baselineReport.meanRecallAtK(),
                fullReport.meanPrecisionAtK() - baselineReport.meanPrecisionAtK(),
                fullReport.meanNdcgAtK() - baselineReport.meanNdcgAtK(),
                fullReport.meanInvestigationSuccessRate() - baselineReport.meanInvestigationSuccessRate());
    }
}
