package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * 분봉 차트 응답 DTO.
 * 
 * API: ka10080 (/api/dostk/chart)
 * 담당: Vector (Technical Agent)
 */
public record MinuteChartResponse(
        String stockCode,
        List<MinuteCandle> candles) {
    /**
     * 분봉 캔들 데이터.
     */
    public record MinuteCandle(
            String time, // 체결시간 (HHMMSS)
            long closePrice, // 종가
            long openPrice, // 시가
            long highPrice, // 고가
            long lowPrice, // 저가
            long volume, // 거래량
            long accumulatedVolume, // 누적거래량
            long priceChange, // 전일대비
            String changeSign // 전일대비기호
    ) {
        /**
         * 양봉 여부.
         */
        public boolean isBullish() {
            return closePrice > openPrice;
        }

        /**
         * 음봉 여부.
         */
        public boolean isBearish() {
            return closePrice < openPrice;
        }

        /**
         * 거래량 급증 여부 (평균 대비 2배 이상).
         */
        public boolean isVolumeSpike(long averageVolume) {
            return volume >= averageVolume * 2;
        }
    }
}
