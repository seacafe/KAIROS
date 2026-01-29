package com.kairos.trading.domain.strategy.agent;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.strategy.dto.StrategyDecisionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nexus Agent (전략가).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NexusAgent {

    private final NexusAiClient nexusAiClient;

    public StrategyDecisionDto decide(String stockCode, String stockName, List<AgentResponse> reports,
            String preference) {
        log.info("[Nexus] 전략 회의 시작 - {} ({}), 성향: {}, 보고서 수: {}", stockName, stockCode, preference, reports.size());

        try {
            String reportsSummary = reports.stream()
                    .map(r -> String.format("[%s] %s (Score: %d) - %s",
                            r.agentName(), r.decision(), r.score(), r.reason()))
                    .collect(Collectors.joining("\n"));

            // LLM 호출
            return nexusAiClient.decide(preference, stockCode, stockName, reportsSummary);

        } catch (Exception e) {
            log.error("[Nexus] 전략 수립 실패: {}", e.getMessage(), e);
            // 에러 시 보수적 판단 (8개 필드)
            return new StrategyDecisionDto(
                    "WATCH", // decision
                    0, // finalScore
                    "HIGH", // riskLevel
                    0.0, // positionSize
                    0, // targetPrice
                    0, // stopLossPrice
                    "전략 수립 중 오류 발생: " + e.getMessage(), // reasoning
                    null // dissent
            );
        }
    }
}
