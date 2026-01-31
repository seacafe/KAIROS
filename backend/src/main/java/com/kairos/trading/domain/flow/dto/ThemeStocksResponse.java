package com.kairos.trading.domain.flow.dto;

import java.util.List;

/**
 * 테마 구성 종목 응답 (ka90002).
 * Nexus 전략가가 테마 연관 종목 분석에 활용.
 */
public record ThemeStocksResponse(
        String themeCode,
        String themeName,
        List<ThemeStock> stocks) {
    /**
     * 테마 구성 종목.
     */
    public record ThemeStock(
            String stockCode,
            String stockName,
            int currentPrice,
            int priceChange,
            double changeRate,
            long volume) {
    }
}
