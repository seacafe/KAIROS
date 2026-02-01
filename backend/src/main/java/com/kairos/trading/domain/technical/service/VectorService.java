package com.kairos.trading.domain.technical.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.common.client.KiwoomClient;
import com.kairos.trading.domain.technical.dto.MovingAverage;
import com.kairos.trading.domain.technical.dto.PriceTimeSeriesResponse;
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
 * 4. 이동평균선(SMA) 데이터 계산 (Kiwoom API 연동)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorService {

    private final VectorAiClient vectorAiClient;
    private final NanoBananaCalculator nanoBananaCalculator;
    private final KiwoomClient kiwoomClient;

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

    /**
     * 종목의 5일, 20일, 60일 단순이동평균(SMA)을 계산한다.
     * Kiwoom API (ka10005)를 사용한다.
     */
    public MovingAverage calculateMovingAverages(String stockCode) {
        try {
            // TODO: 실제 토큰 관리 로직 적용 필요. 현재는 더미 토큰 사용.
            String dummyToken = "vector-agent-token";
            var response = kiwoomClient.getPriceTimeSeries(dummyToken, stockCode, "D");

            if (response == null || response.timeSeries() == null || response.timeSeries().isEmpty()) {
                log.warn("[Vector] 시계열 데이터 없음: {}", stockCode);
                return new MovingAverage(0, 0, 0);
            }

            var timeSeries = response.timeSeries();
            double ma5 = calculateSma(timeSeries, 5);
            double ma20 = calculateSma(timeSeries, 20);
            double ma60 = calculateSma(timeSeries, 60);

            return new MovingAverage(ma5, ma20, ma60);

        } catch (Exception e) {
            log.error("[Vector] SMA 계산 실패: {} - {}", stockCode, e.getMessage());
            return new MovingAverage(0, 0, 0);
        }
    }

    private double calculateSma(java.util.List<PriceTimeSeriesResponse.TimeSeriesData> data, int period) {
        if (data.size() < period) {
            return 0.0;
        }

        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += data.get(i).closePrice();
        }
        return sum / period;
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
