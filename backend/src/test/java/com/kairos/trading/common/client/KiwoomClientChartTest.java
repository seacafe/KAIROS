package com.kairos.trading.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.domain.technical.dto.DailyChartResponse;
import com.kairos.trading.domain.technical.dto.MinuteChartResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KiwoomClient 차트 API 테스트.
 * 
 * 차트 응답 파싱 로직 검증.
 */
class KiwoomClientChartTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("일봉 차트 DTO 테스트")
    class DailyChartTest {

        @Test
        @DisplayName("DailyCandle 양봉 판별")
        void dailyCandle_bullish() {
            var candle = new DailyChartResponse.DailyCandle(
                    "20260131",
                    75000L, // 종가
                    70000L, // 시가
                    76000L, // 고가
                    69000L, // 저가
                    1000000L,
                    75000000000L,
                    2.5,
                    3000L,
                    "2");

            assertThat(candle.isBullish()).isTrue();
            assertThat(candle.isBearish()).isFalse();
            assertThat(candle.bodySize()).isEqualTo(5000L);
            assertThat(candle.totalRange()).isEqualTo(7000L);
        }

        @Test
        @DisplayName("DailyCandle 음봉 판별")
        void dailyCandle_bearish() {
            var candle = new DailyChartResponse.DailyCandle(
                    "20260131",
                    68000L, // 종가
                    72000L, // 시가
                    73000L, // 고가
                    67000L, // 저가
                    1500000L,
                    100000000000L,
                    3.0,
                    -4000L,
                    "5");

            assertThat(candle.isBullish()).isFalse();
            assertThat(candle.isBearish()).isTrue();
            assertThat(candle.bodySize()).isEqualTo(4000L);
        }
    }

    @Nested
    @DisplayName("분봉 차트 DTO 테스트")
    class MinuteChartTest {

        @Test
        @DisplayName("MinuteCandle 거래량 급증 판별")
        void minuteCandle_volumeSpike() {
            var candle = new MinuteChartResponse.MinuteCandle(
                    "093000",
                    75500L,
                    75000L,
                    76000L,
                    74500L,
                    500000L, // 현재 거래량
                    2000000L,
                    500L,
                    "2");

            // 평균 거래량 200000일 때 (2배 이상)
            assertThat(candle.isVolumeSpike(200000L)).isTrue();
            // 평균 거래량 300000일 때 (2배 미만)
            assertThat(candle.isVolumeSpike(300000L)).isFalse();
        }

        @Test
        @DisplayName("MinuteCandle 양봉/음봉 판별")
        void minuteCandle_direction() {
            var bullishCandle = new MinuteChartResponse.MinuteCandle(
                    "093000", 75500L, 75000L, 76000L, 74500L,
                    100000L, 500000L, 500L, "2");

            var bearishCandle = new MinuteChartResponse.MinuteCandle(
                    "093500", 74000L, 75000L, 75500L, 73500L,
                    150000L, 650000L, -1000L, "5");

            assertThat(bullishCandle.isBullish()).isTrue();
            assertThat(bearishCandle.isBearish()).isTrue();
        }
    }
}
