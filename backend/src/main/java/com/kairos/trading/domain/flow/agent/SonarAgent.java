package com.kairos.trading.domain.flow.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.flow.dto.FlowAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sonar Agent (수급 분석가).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SonarAgent {

    private final SonarAiClient sonarAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentResponse analyze(String stockCode, String stockName, String tradeData, String programTradeData) {
        log.info("[Sonar] 분석 시작 - {} ({})", stockName, stockCode);

        try {
            // JSON Parsing
            JsonNode tradeNode = objectMapper.readTree(tradeData);
            JsonNode progNode = objectMapper.readTree(programTradeData);

            long foreignNet = tradeNode.path("foreignNet").asLong(0);
            long institutionNet = tradeNode.path("institutionNet").asLong(0);
            long programNet = progNode.path("programNet").asLong(0);
            double programRatio = progNode.path("programRatio").asDouble(0.0);

            // LLM 호출
            FlowAnalysisDto result = sonarAiClient.analyzeFlow(
                    stockCode,
                    stockName,
                    foreignNet,
                    institutionNet,
                    programNet,
                    programRatio);

            return mapToAgentResponse(result);

        } catch (Exception e) {
            log.error("[Sonar] 분석 실패: {}", e.getMessage(), e);
            return new AgentResponse(
                    "Sonar",
                    50,
                    "WATCH",
                    "수급 분석 실패: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    private AgentResponse mapToAgentResponse(FlowAnalysisDto dto) {
        String decision = "WATCH";
        int score = 50;

        if (dto.isDistribution() || dto.isSelling()) {
            decision = "REJECT";
            score = 20;
        } else if (dto.isDoubleBuy()) {
            decision = "BUY";
            score = 90;
        } else if (dto.isAccumulation()) {
            decision = "BUY";
            score = 80;
        } else if ("InstitutionBuy".equals(dto.flowType()) || "ForeignBuy".equals(dto.flowType())) {
            score = 70;
        }

        return new AgentResponse(
                "Sonar",
                score,
                decision,
                String.format("[%s] %s", dto.flowType(), dto.summary()),
                Map.of(
                        "flowType", dto.flowType(),
                        "programNet", dto.programNet(),
                        "dto", dto));
    }
}
