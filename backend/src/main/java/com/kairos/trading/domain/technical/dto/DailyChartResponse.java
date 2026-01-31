package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * 일봉 차트 응답 DTO.
 * 
 * API: ka10081 (/api/dostk/chart)
 * 담당: Vector (Technical Agent)
 */
public record DailyChartResponse(
        String stockCode,
        List<DailyCandle> candles) {
    /**
     * 일봉 캔들 데이터.
     */
    public record DailyCandle(
            String date, // 일자 (YYYYMMDD)
            long closePrice, // 종가
            long openPrice, // 시가
            long highPrice, // 고가
            long lowPrice, // 저가
            long volume, // 거래량
            long tradingValue, // 거래대금
            double turnoverRate, // 거래회전율
            long priceChange, // 전일대비
            String changeSign // 전일대비기호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
    ) {
        /**
         * 양봉 여부 (종가 > 시가).
         */
        public boolean isBullish() {
            return closePrice > openPrice;
        }

        /**
         * 음봉 여부 (종가 < 시가).
         */
        public boolean isBearish() {
            return closePrice < openPrice;
        }

        /**
         * 캔들 몸통 크기.
         */
        public long bodySize() {
            return Math.abs(closePrice - openPrice);
        }

        /**
         * 캔들 전체 길이 (고가 - 저가).
         */
        public long totalRange() {
            return highPrice - lowPrice;
        }
    }
}
