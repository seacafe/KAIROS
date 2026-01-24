package com.kairos.trading.domain.technical.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NanoBanana 패턴 계산기 (Pure Java).
 * 
 * 이평선(5/20/60) 수렴 후 거래량 폭발을 동반한 확산 패턴을 감지한다.
 * AI 추론 없이 실시간으로 동작해야 하므로 순수 Java로 구현.
 */
@Slf4j
@Service
public class NanoBananaCalculator {

    /**
     * 이평선 수렴도 계산 (0 ~ 1).
     * 1에 가까울수록 이평선들이 밀집되어 있음.
     * 
     * @param ma5  5일 이동평균
     * @param ma20 20일 이동평균
     * @param ma60 60일 이동평균
     * @return 수렴도 (0 ~ 1)
     */
    public double calculateConvergence(double ma5, double ma20, double ma60) {
        if (ma60 <= 0)
            return 0;

        // 이평선 간 편차 계산
        double avgMa = (ma5 + ma20 + ma60) / 3.0;
        double deviation5 = Math.abs(ma5 - avgMa) / avgMa;
        double deviation20 = Math.abs(ma20 - avgMa) / avgMa;
        double deviation60 = Math.abs(ma60 - avgMa) / avgMa;

        double avgDeviation = (deviation5 + deviation20 + deviation60) / 3.0;

        // 편차가 작을수록 수렴도가 높음 (5% 이내면 완전 수렴)
        double convergence = Math.max(0, 1 - (avgDeviation * 20));

        log.debug("[NanoBanana] 수렴도 계산: MA5={}, MA20={}, MA60={} → {}",
                ma5, ma20, ma60, convergence);

        return convergence;
    }

    /**
     * 이격도 계산.
     * 현재가가 20일 이평선 대비 얼마나 떨어져 있는지 백분율로 표시.
     * 
     * @param currentPrice 현재가
     * @param ma20         20일 이동평균
     * @return 이격도 (예: 5.0 = 5% 위에 있음, -3.0 = 3% 아래)
     */
    public double calculateDeviation(double currentPrice, double ma20) {
        if (ma20 <= 0)
            return 0;
        return ((currentPrice - ma20) / ma20) * 100;
    }

    /**
     * 거래량 급증률 계산.
     * 
     * @param todayVolume 오늘 거래량
     * @param avgVolume   평균 거래량 (5일 또는 20일)
     * @return 거래량 비율 (예: 2.5 = 250%)
     */
    public double calculateVolumeRatio(long todayVolume, long avgVolume) {
        if (avgVolume <= 0)
            return 0;
        return (double) todayVolume / avgVolume;
    }

    /**
     * NanoBanana 패턴 존재 여부 확인.
     * 
     * @param convergence 이평선 수렴도
     * @param volumeRatio 거래량 비율
     * @param isBullish   상승 추세 여부 (MA5 > MA20 > MA60)
     * @return 패턴 존재 여부
     */
    public boolean isNanoBananaPattern(double convergence, double volumeRatio, boolean isBullish) {
        // 조건: 수렴도 0.7 이상, 거래량 2배 이상, 상승 추세
        boolean isPattern = convergence >= 0.7 && volumeRatio >= 2.0 && isBullish;

        if (isPattern) {
            log.info("[NanoBanana] 🍌 패턴 감지! 수렴도={}, 거래량={}배",
                    String.format("%.2f", convergence),
                    String.format("%.1f", volumeRatio));
        }

        return isPattern;
    }

    /**
     * 정배열 여부 확인 (MA5 > MA20 > MA60).
     */
    public boolean isBullishAlignment(double ma5, double ma20, double ma60) {
        return ma5 > ma20 && ma20 > ma60;
    }

    /**
     * 역배열 여부 확인 (MA5 < MA20 < MA60).
     */
    public boolean isBearishAlignment(double ma5, double ma20, double ma60) {
        return ma5 < ma20 && ma20 < ma60;
    }

    /**
     * 이동평균 계산.
     * 
     * @param prices 가격 리스트 (최신이 마지막)
     * @param period 기간 (5, 20, 60 등)
     * @return 이동평균
     */
    public double calculateMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            return 0;
        }

        int startIndex = prices.size() - period;
        double sum = 0;
        for (int i = startIndex; i < prices.size(); i++) {
            sum += prices.get(i);
        }

        return sum / period;
    }
}
