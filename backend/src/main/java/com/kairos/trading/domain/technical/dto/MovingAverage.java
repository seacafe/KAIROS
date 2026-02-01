package com.kairos.trading.domain.technical.dto;

/**
 * 이동평균(SMA) 계산 결과 DTO.
 */
public record MovingAverage(
        double ma5,
        double ma20,
        double ma60) {
}
