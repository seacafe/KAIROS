package com.kairos.trading.domain.flow.dto;

import java.util.List;

/**
 * 테마 그룹별 응답 (ka90001).
 * Nexus 전략가가 테마 분석에 활용.
 */
public record ThemeGroupResponse(
        List<ThemeGroup> themes) {
    /**
     * 테마 그룹.
     */
    public record ThemeGroup(
            String themeCode,
            String themeName,
            int stockCount,
            double avgChangeRate,
            long avgVolume) {
    }
}
