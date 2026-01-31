package com.kairos.trading.domain.execution.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TrailingStopServiceTest {

    @InjectMocks
    private TrailingStopService trailingStopService;

    @Test
    @DisplayName("calculateTrailingStop: 수익률이 시작 조건(3%) 미만이면 원래 손절가를 유지해야 한다")
    void calculateTrailingStop_ShouldKeepOriginal_WhenProfitIsLow() {
        // Given
        long originalStopLoss = 10000;
        long currentPrice = 10200; // 2% 수익
        long targetPrice = 11000;

        // When
        long result = trailingStopService.calculateTrailingStop(originalStopLoss, currentPrice, targetPrice);

        // Then
        assertThat(result).isEqualTo(originalStopLoss);
    }

    @Test
    @DisplayName("calculateTrailingStop: 수익률이 시작 조건 이상이면 손절가를 상향 조정해야 한다")
    void calculateTrailingStop_ShouldUpdateStopLoss_WhenProfitIsHigh() {
        // Given
        long originalStopLoss = 10000;
        long currentPrice = 10900; // 10500 대비 약 3.8% 수익 (Threshold 3.0% 초과)
        long targetPrice = 11000;

        // When
        long result = trailingStopService.calculateTrailingStop(originalStopLoss, currentPrice, targetPrice);

        // Then
        // 예상 진입가 = (10000 + 11000) / 2 = 10500
        // Gap = 11000 - 10500 = 500
        // Trailing Distance = 500 * 0.5 = 250
        // New Stop Loss = 10900 - 250 = 10650
        assertThat(result).isGreaterThan(originalStopLoss);
        assertThat(result).isEqualTo(10650);
    }

    @Test
    @DisplayName("calculateTrailingStop: 새로운 손절가가 원래 손절가보다 낮으면 원래 값을 유지해야 한다")
    void calculateTrailingStop_ShouldKeepMax_WhenNewStopLow() {
        // Given
        long originalStopLoss = 10000;
        long currentPrice = 10400; // 4% 수익 (진입가 10000 가정 시)
        // estimatedEntry logic depends on input.
        // Let's assume input such that calculated is lower.
        // estimatedEntry = (10000 + 10800) / 2 = 10400 -> profit 0? No.

        // Let's force a scenario.
        // If currentPrice drops but is still above originalStopLoss/Target,
        // logic is purely based on math.

        // Case where calculated new stop loss is lower than original.
        // original=10000, current=10350, target=11000
        // est = 10500. profit = (10350-10500)/10500 -> negative?
        // Wait, estimatedEntry logic is (original + target) / 2.

        // Let's try to match the logic exactly.
        // original=10000, target=11000 -> estimated=10500.
        // current=10815 (+3%).
        // Gap=500, Dist=250.
        // New=10815 - 250 = 10565 > 10000.

        // If current is just above 3% threshold.
        // current = 10500 * 1.03 = 10815.
        // What if calculated trailing stop is remarkably low?
        // It relies on currentPrice - trailingDistance.
        // trailingDistance is fixed based on target and estimated entry.

        // Test ensures Math.max works.
        long targetPrice = 11000;

        // To trigger 'lower new stop loss', we need currentPrice - dist < original.
        // 10815 - 250 = 10565 > 10000.

        // It seems structurally designed to raise stop loss.
        // But if price drops after raising?
        // This method is stateless. It takes 'originalStopLoss'.
        // If 'originalStopLoss' passed in is already a raised value (e.g. 10500).
        // And current price is 10815.
        // New calc = 10565.
        // Math.max(10500, 10565) -> 10565.

        // If passed original is 10600 (higher than calc).
        // New calc = 10565.
        // Result should be 10600.

        long result = trailingStopService.calculateTrailingStop(10600, 10815, 11000);
        assertThat(result).isEqualTo(10600);
    }

    @Test
    @DisplayName("calculateAtrTrailingStop: ATR 기반으로 손절가를 정확히 계산해야 한다")
    void calculateAtrTrailingStop_ShouldCalculateCorrectly() {
        // Given
        double atr = 1000.0;
        long currentPrice = 50000;
        double multiplier = 2.0;

        // When
        long result = trailingStopService.calculateAtrTrailingStop(atr, currentPrice, multiplier);

        // Then
        assertThat(result).isEqualTo(48000);
    }

    @Test
    @DisplayName("calculateAtrTrailingStop: ATR이 0 이하이면 예외를 던져야 한다")
    void calculateAtrTrailingStop_ShouldThrowException_WhenAtrIsZero() {
        // Given
        double atr = 0.0;
        long currentPrice = 50000;
        double multiplier = 2.0;

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            trailingStopService.calculateAtrTrailingStop(atr, currentPrice, multiplier);
        });
    }

    @Test
    @DisplayName("calculateTrailingStop: Gap Down(현재가 < 목표가) 발생 시 손절가는 목표가보다 낮게 설정되어야 한다 (즉, 즉시 손절 가능성)")
    void calculateTrailingStop_ShouldHandleGapDown() {
        // Given
        long originalStopLoss = 10000;
        long targetPrice = 11000;
        long currentPrice = 10800; // Gap Down situation logic check (if intended)
        // Wait, typical gap down means Open < Close_prev significantly.
        // Here, if currentPrice < targetPrice, but > stopLoss.
        // It should just return original stop loss or calculated one.
        // But if currentPrice is suspiciously low?

        // Let's test negative gap logic if applicable, or just extreme volatility
        // check.
        // If currentPrice < originalStopLoss?
        long crashPrice = 9000;

        // When
        long result = trailingStopService.calculateTrailingStop(originalStopLoss, crashPrice, targetPrice);

        // Then
        // Should not be lower than original (which implies selling immediately)
        // Or wait, if price crashed below stop loss, result should still be stop loss
        // trigger.
        // The service calculates the *threshold*.
        assertThat(result).isEqualTo(originalStopLoss);
    }
}
