package com.kairos.trading.domain.sentiment.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.sentiment.dto.MarketSentimentDto;
import com.kairos.trading.domain.sentiment.agent.ResonanceAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resonance 시장 심리 분석 서비스.
 * 
 * 역할:
 * 1. Market Heat Score (0~100) 산출
 * 2. Risk-On/Risk-Off 판단
 * 3. 신규 진입 차단(Veto) 권한
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResonanceService {

    private final ResonanceAiClient resonanceAiClient;

    /**
     * 시장 심리를 AI로 분석한다.
     */
    public MarketSentimentDto analyzeMarket(
            double nasdaqChange,
            double usdKrw,
            double vix,
            double kospiChange,
            double oilPrice,
            String newsHeadlines) {
        log.debug("[Resonance] 시장 심리 분석 시작");

        var result = resonanceAiClient.analyzeMarket(
                nasdaqChange, usdKrw, vix, kospiChange, oilPrice, newsHeadlines);

        // 결과 로깅
        log.info("[Resonance] Market Heat: {} ({})",
                result.marketHeatScore(), result.sentiment());

        if (result.isRiskOff()) {
            log.warn("[Resonance] ⚠️ RISK-OFF: 신규 진입 차단");
        }

        return result;
    }

    /**
     * 진입 가능 여부 판단 (Veto).
     */
    public boolean isEntryAllowed(int marketHeatScore, String strategyMode) {
        int threshold = switch (strategyMode) {
            case "STABLE" -> 50;
            case "NEUTRAL" -> 40;
            case "AGGRESSIVE" -> 30;
            default -> 40;
        };

        return marketHeatScore >= threshold;
    }

    /**
     * AgentResponse 형식으로 변환하여 반환한다.
     */
    public AgentResponse analyzeAndGetResponse(
            double nasdaqChange,
            double usdKrw,
            double vix,
            double kospiChange,
            double oilPrice,
            String newsHeadlines) {
        var result = analyzeMarket(
                nasdaqChange, usdKrw, vix, kospiChange, oilPrice, newsHeadlines);

        String decision = result.isRiskOff() ? "REJECT" : result.marketHeatScore() >= 60 ? "BUY" : "WATCH";

        return new AgentResponse(
                "Resonance",
                result.marketHeatScore(),
                decision,
                result.summary(),
                Map.of(
                        "sentiment", result.sentiment(),
                        "riskStatus", result.riskStatus(),
                        "vetoActive", result.isRiskOff()));
    }
}
