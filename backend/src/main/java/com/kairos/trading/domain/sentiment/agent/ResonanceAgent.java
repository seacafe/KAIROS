package com.kairos.trading.domain.sentiment.agent;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.sentiment.dto.MarketSentimentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resonance Agent (시장 심리 분석가).
 * 
 * 역할:
 * - 거시경제 지표(나스닥, 환율, 유가) 및 뉴스 헤드라인 분석
 * - 시장의 공포/탐욕 지수(Market Heat) 산출
 * - Risk-On / Risk-Off 상태 판별
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResonanceAgent {

    private final ResonanceAiClient resonanceAiClient;

    /**
     * 시장 데이터를 분석하여 전반적인 투자 분위기를 판단한다.
     * 
     * @param nasdaqChange  나스닥 선물 등락률
     * @param usdKrw        원달러 환율
     * @param vix           VIX 지수
     * @param kospiChange   코스피 선물 등락률
     * @param oilPrice      WTI 유가
     * @param newsHeadlines 주요 뉴스 헤드라인
     * @return 표준화된 분석 결과
     */
    public AgentResponse analyze(double nasdaqChange, double usdKrw, double vix, double kospiChange, double oilPrice,
            String newsHeadlines) {
        log.info("[Resonance] 시장 분석 시작 (Nasdaq: {}%, USD: {})", nasdaqChange, usdKrw);

        try {
            // LLM 호출
            MarketSentimentDto result = resonanceAiClient.analyzeMarket(nasdaqChange, usdKrw, vix, kospiChange,
                    oilPrice, newsHeadlines);

            // 매핑 및 반환
            return mapToAgentResponse(result);

        } catch (Exception e) {
            log.error("[Resonance] 분석 실패: {}", e.getMessage(), e);
            return new AgentResponse(
                    "Resonance",
                    50,
                    "WATCH",
                    "시장 분석 실패: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    private AgentResponse mapToAgentResponse(MarketSentimentDto dto) {
        String decision = "WATCH";
        int score = dto.marketHeatScore();

        // Risk-Off 상태이거나 공포 구간이면 매수 금지 (REJECT)
        if (dto.isRiskOff() || score < 40) {
            decision = "REJECT"; // "Veto" in Nexus logic
        } else if (dto.isRiskOn() && score >= 60) {
            decision = "BUY";
        }

        return new AgentResponse(
                "Resonance",
                score,
                decision,
                String.format("[%s] %s (Heat: %d)", dto.sentiment(), dto.summary(), score),
                Map.of(
                        "riskStatus", dto.riskStatus(),
                        "nasdaqImpact", dto.nasdaqImpact() != null ? dto.nasdaqImpact() : "",
                        "dto", dto));
    }
}
