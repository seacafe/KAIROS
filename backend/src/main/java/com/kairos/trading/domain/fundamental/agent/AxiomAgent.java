package com.kairos.trading.domain.fundamental.agent;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.fundamental.dto.FundamentalAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Axiom Agent (펀더멘털 분석가).
 * 
 * 역할:
 * - 재무제표 및 기업 가치 분석
 * - 상장폐지 위험 종목 필터링 (Veto)
 * - 우량/저평가 종목 발굴
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AxiomAgent {

    private final AxiomAiClient axiomAiClient;

    /**
     * 재무 데이터를 분석하여 의사결정을 내린다.
     * 
     * @param stockCode     종목코드
     * @param stockName     종목명
     * @param financialData 재무 데이터 요약 (JSON)
     * @return 표준화된 분석 결과
     */
    public AgentResponse analyze(String stockCode, String stockName, String financialData) {
        log.info("[Axiom] 분석 시작 - {} ({})", stockName, stockCode);

        try {
            // LLM 호출
            FundamentalAnalysisDto result = axiomAiClient.analyzeFinancial(stockCode, stockName, financialData);

            // 매핑 및 반환
            return mapToAgentResponse(result);

        } catch (Exception e) {
            log.error("[Axiom] 분석 실패: {}", e.getMessage(), e);
            return new AgentResponse(
                    "Axiom",
                    50,
                    "WATCH",
                    "재무 분석 실패: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    private AgentResponse mapToAgentResponse(FundamentalAnalysisDto dto) {
        String decision = "WATCH";
        int score = 50;

        if (dto.isRejected()) {
            decision = "REJECT";
            score = 0;
        } else if (dto.isBlueChip()) {
            decision = "BUY";
            score = 90;
        } else if (dto.isUndervalued()) {
            decision = "BUY";
            score = 85;
        } else if (dto.isHighRisk()) {
            decision = "WATCH"; // High Risk지만 Reject는 아닌 경우
            score = 40;
        } else if ("PASS".equals(dto.decision())) {
            score = 70;
        }

        return new AgentResponse(
                "Axiom",
                score,
                decision,
                String.format("%s (PER: %.2f, PBR: %.2f)", dto.summary(), dto.per(), dto.pbr()),
                Map.of(
                        "per", dto.per() != null ? dto.per() : 0.0,
                        "pbr", dto.pbr() != null ? dto.pbr() : 0.0,
                        "riskLevel", dto.riskLevel(),
                        "dto", dto));
    }
}
