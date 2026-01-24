package com.kairos.trading.domain.news.dto;

import java.util.List;

/**
 * 뉴스/공시 분석 결과 DTO.
 * Sentinel 에이전트가 반환하는 분석 결과.
 */
public record NewsAnalysisDto(
        String stockCode,
        String stockName,
        List<String> keywords,
        String sentiment, // Positive, Negative, Neutral
        int materialStrength, // 재료 강도 (-100 ~ 100)
        String urgency, // High, Low
        boolean killSwitch, // Kill Switch 발동 여부
        String summary // 분석 요약
) {
    /**
     * Kill Switch 발동이 필요한지 확인
     */
    public boolean requiresKillSwitch() {
        return killSwitch || materialStrength <= -80;
    }

    /**
     * 호재인지 확인
     */
    public boolean isPositive() {
        return "Positive".equals(sentiment) && materialStrength > 0;
    }

    /**
     * 악재인지 확인
     */
    public boolean isNegative() {
        return "Negative".equals(sentiment) || materialStrength < 0;
    }

    /**
     * Kill Switch 키워드 목록
     */
    public static final List<String> KILL_SWITCH_KEYWORDS = List.of(
            "횡령", "배임", "감자", "상장폐지", "거래정지", "불성실공시", "분식회계");
}
