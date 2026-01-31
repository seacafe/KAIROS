package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * 등락률 상위 종목 응답 (ka10027).
 * Vector 에이전트가 모멘텀 감지에 활용.
 */
public record ChangeRateTop(
        List<ChangeRateStock> stocks) {
    /**
     * 등락률 상위 종목.
     */
    public record ChangeRateStock(
            int rank,
            String stockCode,
            String stockName,
            int currentPrice,
            String priceSign,
            int priceChange,
            double changeRate,
            long volume,
            int prevClose) {
    }
}
