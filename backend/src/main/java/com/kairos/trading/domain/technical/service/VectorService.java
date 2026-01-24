package com.kairos.trading.domain.technical.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.technical.dto.TechnicalAnalysisDto;
import com.kairos.trading.domain.technical.agent.VectorAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Vector 기술적 분석 서비스.
 * 
 * 역할:
 * 1. NanoBanana 패턴 감지
 * 2. 호가창 분석 (허매수벽 감지)
 * 3. 정밀 진입가/목표가 산출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorService {

    private final VectorAiClient vectorAiClient;
    private final NanoBananaCalculator nanoBananaCalculator;

    /**
     * 차트 및 호가창을 AI로 분석한다.
     */
    public TechnicalAnalysisDto analyzeChart(
            String stockCode,
            String stockName,
            long currentPrice,
            double ma5,
            double ma20,
            double ma60,
            long todayVolume,
            long avgVolume,
            String orderBookSnapshot) {
        log.debug("[Vector] 차트 분석 시작: {} ({})", stockName, stockCode);

        // AI 분석 수행
        var result = vectorAiClient.analyzeChart(
                stockCode, stockName, currentPrice,
                ma5, ma20, ma60,
                todayVolume, avgVolume,
                orderBookSnapshot);

        // 결과 로깅
        if (result.isNanoBanana()) {
            log.info("[Vector] 🍌 NanoBanana 패턴 감지: {} (점수: {})",
                    stockName, result.entryScore());
        }

        if (result.isFakeWall()) {
            log.warn("[Vector] ⚠️ 허매수벽 의심: {}", stockName);
        }

        return result;
    }

    /**
     * NanoBanana 패턴 감지 (Java 로직).
     */
    public boolean detectNanoBananaPattern(
            double ma5, double ma20, double ma60,
            long todayVolume, long avgVolume) {
        double convergence = nanoBananaCalculator.calculateConvergence(ma5, ma20, ma60);
        double volumeRatio = nanoBananaCalculator.calculateVolumeRatio(todayVolume, avgVolume);
        boolean isBullish = nanoBananaCalculator.isBullishAlignment(ma5, ma20, ma60);

        return convergence >= 0.7 && volumeRatio >= 2.0 && isBullish;
    }

    /**
     * AgentResponse 형식으로 변환하여 반환한다.
     */
    public AgentResponse analyzeAndGetResponse(
            String stockCode,
            String stockName,
            long currentPrice,
            double ma5, double ma20, double ma60,
            long todayVolume, long avgVolume,
            String orderBookSnapshot) {
        var result = analyzeChart(
                stockCode, stockName, currentPrice,
                ma5, ma20, ma60,
                todayVolume, avgVolume,
                orderBookSnapshot);

        String decision = determineDecision(result);

        return new AgentResponse(
                "Vector",
                result.entryScore(),
                decision,
                result.summary(),
                Map.of(
                        "pattern", result.pattern(),
                        "entryPrice", result.entryPrice(),
                        "targetPrice", result.targetPrice(),
                        "stopLossPrice", result.stopLossPrice(),
                        "isFakeWall", result.isFakeWall()));
    }

    private String determineDecision(TechnicalAnalysisDto result) {
        if (result.isTrap()) {
            return "REJECT";
        } else if (result.isNanoBanana() && result.entryScore() >= 70) {
            return "BUY";
        } else if (result.entryScore() >= 50) {
            return "WATCH";
        } else {
            return "REJECT";
        }
    }
}
