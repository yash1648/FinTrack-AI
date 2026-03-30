package com.grim.backend.analysis.service;


import com.grim.backend.analysis.ai.OllamaAnalysisClient;
import com.grim.backend.analysis.common.InsightPromptBuilder;
import com.grim.backend.analysis.common.SpendingAggregator;
import com.grim.backend.analysis.dto.AiInsightResult;
import com.grim.backend.analysis.dto.AnomalyResponse;
import com.grim.backend.analysis.dto.InsightData;
import com.grim.backend.analysis.dto.ProjectionResponse;
import com.grim.backend.transaction.entity.Transaction;
import com.grim.backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String,Object> redisTemplate;

    private final SpendingAggregator aggregator;
    private final AnomalyDetectionService anomalyService;
    private final ProjectionService projectionService;

    private final InsightPromptBuilder promptBuilder;
    private final OllamaAnalysisClient aiClient;

    public InsightData getInsights(UUID userId) {

        String cacheKey = "insights:" + userId;

        InsightData cached = (InsightData) redisTemplate.opsForValue().get(cacheKey);

        if(cached != null) return cached;
        LocalDate startDate = LocalDate.now().minusDays(90);
        List<Transaction> txs =
                transactionRepository.findLast90Days(userId,startDate);

        if(!hasAtLeast30DistinctTransactionDays(txs)) {
            return InsightData.builder()
                    .sufficient(false)
                    .message("Add at least 30 days of transactions to unlock AI insights.")
                    .build();
        }

        Map<String, BigDecimal> totals =
                aggregator.aggregateByCategory(txs);

        String prompt = promptBuilder.build(totals);

        AiInsightResult aiResult = aiClient.analyze(prompt);

        List<AnomalyResponse> anomalies = anomalyService.detect(txs);

        ProjectionResponse projection = projectionService.project(txs);

        InsightData data = InsightData.builder()
                .sufficient(true)
                .patterns(aiResult.getPatterns())
                .recommendations(aiResult.getRecommendations())
                .anomalies(anomalies)
                .projectedMonthlyExpense(projection.getProjected())
                .cachedAt(Instant.now())
                .build();

        redisTemplate.opsForValue()
                .set(cacheKey, data, Duration.ofHours(6));

        return data;
    }

    public List<AnomalyResponse> getAnomalies(UUID userId) {
        LocalDate startDate = LocalDate.now().minusDays(90);
        List<Transaction> txs = transactionRepository.findLast90Days(userId,startDate);
        return anomalyService.detect(txs);
    }

    public ProjectionResponse getProjection(UUID userId) {
        LocalDate startDate = LocalDate.now().minusDays(90);
        List<Transaction> txs = transactionRepository.findLast90Days(userId,startDate);
        return projectionService.project(txs);
    }

    private boolean hasAtLeast30DistinctTransactionDays(List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) {
            return false;
        }

        long distinctDays = txs.stream()
                .map(Transaction::getDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return distinctDays >= 30;
    }
}