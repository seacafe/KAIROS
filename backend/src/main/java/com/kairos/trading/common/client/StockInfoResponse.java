package com.kairos.trading.common.client;

/**
 * 키움 API 주식 기본정보 응답 DTO (ka10001).
 */
public record StockInfoResponse(
        String stockCode, // 종목 코드
        String stockName, // 종목명
        long currentPrice, // 현재가
        long previousClose, // 전일 종가
        long openPrice, // 시가
        long highPrice, // 고가
        long lowPrice, // 저가
        long volume, // 거래량
        double changeRate, // 등락률
        double per, // PER
        double pbr, // PBR
        double eps, // EPS
        String marketType // 시장구분 (KOSPI/KOSDAQ)
) {
}
