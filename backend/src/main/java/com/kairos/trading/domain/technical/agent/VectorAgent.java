package com.kairos.trading.domain.technical.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.technical.dto.TechnicalAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Vector Agent (기술적 분석가).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorAgent {

    private final VectorAiClient vectorAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentResponse analyze(String stockCode, String stockName, String chartData, String orderBookData) {
        log.info("[Vector] 분석 시작 - {} ({})", stockName, stockCode);

        try {
            // JSON Parsing
            JsonNode chartNode = objectMapper.readTree(chartData);

            long currentPrice = chartNode.path("currentPrice").asLong(0);
            double ma5 = chartNode.path("ma5").asDouble(0.0);
            double ma20 = chartNode.path("ma20").asDouble(0.0);
            double ma60 = chartNode.path("ma60").asDouble(0.0);
            long volume = chartNode.path("volume").asLong(0);
            long prevVolume = chartNode.path("prevVolume").asLong(0);

            // LLM 호출
            TechnicalAnalysisDto result = vectorAiClient.analyzeChart(
                    stockCode,
                    stockName,
                    currentPrice,
                    ma5,
                    ma20,
                    ma60,
                    volume,
                    prevVolume,
                    orderBookData);

            return mapToAgentResponse(result);

        } catch (Exception e) {
            log.error("[Vector] 분석 실패: {}", e.getMessage(), e);
            return new AgentResponse(
                    "Vector",
                    0,
                    "WATCH",
                    "기술적 분석 실패: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    private AgentResponse mapToAgentResponse(TechnicalAnalysisDto dto) {
        String decision = "WATCH";
        if (dto.hasBuySignal()) {
            decision = "BUY";
        } else if (dto.isTrap() || "Bearish".equals(dto.pattern())) {
            decision = "REJECT";
        }

        int score = dto.entryScore();

        return new AgentResponse(
                "Vector",
                score,
                decision,
                String.format("[%s] %s", dto.pattern(), dto.summary()),
                Map.of(
                        "pattern", dto.pattern(),
                        "entryPrice", dto.entryPrice(),
                        "dto", dto));
    }
}
