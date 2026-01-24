package com.kairos.trading.domain.technical.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NanoBanana 패턴 계산기 테스트.
 */
@DisplayName("NanoBananaCalculator 테스트")
class NanoBananaCalculatorTest {

    private NanoBananaCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NanoBananaCalculator();
    }

    @Test
    @DisplayName("이평선이 밀집되면 수렴도가 높아야 한다")
    void calculateConvergence_shouldBeHigh_whenMAsAreTight() {
        // given: 이평선이 거의 같은 값
        double ma5 = 70000;
        double ma20 = 69800;
        double ma60 = 69500;

        // when
        double convergence = calculator.calculateConvergence(ma5, ma20, ma60);

        // then: 0.7 이상이어야 밀집
        assertThat(convergence).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("이평선이 분산되면 수렴도가 낮아야 한다")
    void calculateConvergence_shouldBeLow_whenMAsAreSpread() {
        // given: 이평선이 많이 벌어짐
        double ma5 = 80000;
        double ma20 = 70000;
        double ma60 = 60000;

        // when
        double convergence = calculator.calculateConvergence(ma5, ma20, ma60);

        // then: 0.5 미만이어야 분산
        assertThat(convergence).isLessThan(0.5);
    }

    @Test
    @DisplayName("거래량이 2배 이상이면 급증으로 판정")
    void calculateVolumeRatio_shouldDetectSurge() {
        // given
        long todayVolume = 30_000_000;
        long avgVolume = 10_000_000;

        // when
        double ratio = calculator.calculateVolumeRatio(todayVolume, avgVolume);

        // then
        assertThat(ratio).isEqualTo(3.0);
    }

    @Test
    @DisplayName("정배열(MA5 > MA20 > MA60)이면 상승 추세")
    void isBullishAlignment_shouldReturnTrue_whenAscending() {
        // given
        double ma5 = 72000;
        double ma20 = 70000;
        double ma60 = 68000;

        // when
        boolean isBullish = calculator.isBullishAlignment(ma5, ma20, ma60);

        // then
        assertThat(isBullish).isTrue();
    }

    @Test
    @DisplayName("역배열(MA5 < MA20 < MA60)이면 하락 추세")
    void isBearishAlignment_shouldReturnTrue_whenDescending() {
        // given
        double ma5 = 68000;
        double ma20 = 70000;
        double ma60 = 72000;

        // when
        boolean isBearish = calculator.isBearishAlignment(ma5, ma20, ma60);

        // then
        assertThat(isBearish).isTrue();
    }

    @Test
    @DisplayName("NanoBanana 패턴: 수렴도 높고, 거래량 폭발, 정배열")
    void isNanoBananaPattern_shouldReturnTrue_whenAllConditionsMet() {
        // given
        double convergence = 0.85;
        double volumeRatio = 2.5;
        boolean isBullish = true;

        // when
        boolean isPattern = calculator.isNanoBananaPattern(convergence, volumeRatio, isBullish);

        // then
        assertThat(isPattern).isTrue();
    }

    @Test
    @DisplayName("NanoBanana 패턴: 거래량 부족 시 미감지")
    void isNanoBananaPattern_shouldReturnFalse_whenLowVolume() {
        // given
        double convergence = 0.85;
        double volumeRatio = 1.2; // 2배 미만
        boolean isBullish = true;

        // when
        boolean isPattern = calculator.isNanoBananaPattern(convergence, volumeRatio, isBullish);

        // then
        assertThat(isPattern).isFalse();
    }

    @Test
    @DisplayName("이동평균 계산")
    void calculateMA_shouldReturnCorrectAverage() {
        // given
        List<Double> prices = List.of(100.0, 102.0, 104.0, 106.0, 108.0);

        // when
        double ma5 = calculator.calculateMA(prices, 5);

        // then
        assertThat(ma5).isEqualTo(104.0);
    }

    @Test
    @DisplayName("이격도: 현재가가 이평선 위에 있으면 양수")
    void calculateDeviation_shouldBePositive_whenAboveMA() {
        // given
        double currentPrice = 73500;
        double ma20 = 70000;

        // when
        double deviation = calculator.calculateDeviation(currentPrice, ma20);

        // then
        assertThat(deviation).isEqualTo(5.0);
    }
}
