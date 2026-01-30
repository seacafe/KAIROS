package com.kairos.trading.domain.fundamental.dto;

public record StockInfoResponse(
        String stockCode,
        String stockName,
        long currentPrice,
        long previousClose,
        long openPrice,
        long highPrice,
        long lowPrice,
        long volume,
        double changeRate,
        double per,
        double pbr,
        double eps,
        String marketType) {
}
