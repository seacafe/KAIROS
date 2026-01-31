package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * 거래량 급증 종목 응답 (ka10023).
 * Vector 에이전트가 급등 종목 스크리닝에 활용.
 */
public record VolumeSpike(
        List<VolumeSpikeStock> stocks) {
    /**
     * 거래량 급증 종목.
     */
    public record VolumeSpikeStock(
            int rank,
            String stockCode,
            String stockName,
            int currentPrice,
            String priceSign, // "2", "5" 등 (상한, 상승...)
            int priceChange,
            double changeRate,
            long volume,
            double volumeRate, // 거래량 비율 (전일 대비 %)
            long turnover // 거래대금
    ) {
    }
}
