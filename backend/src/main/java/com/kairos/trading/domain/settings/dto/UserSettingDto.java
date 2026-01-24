package com.kairos.trading.domain.settings.dto;

/**
 * 사용자 설정 응답 DTO.
 */
public record UserSettingDto(
        String strategyMode, // AGGRESSIVE, NEUTRAL, STABLE
        boolean reEntryAllowed,
        double maxLossPerTrade) {
}
