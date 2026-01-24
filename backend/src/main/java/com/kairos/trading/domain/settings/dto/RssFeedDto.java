package com.kairos.trading.domain.settings.dto;

/**
 * RSS 피드 응답 DTO.
 */
public record RssFeedDto(
        Long id,
        String name,
        String url,
        String category, // DOMESTIC, DISCLOSURE, GLOBAL
        boolean isActive) {
}
