package com.kairos.trading.domain.sentiment.dto;

/**
 * 시장 심리 분석 결과 DTO.
 * Resonance 에이전트가 반환하는 분석 결과.
 */
public record MarketSentimentDto(
        int marketHeatScore, // 0~100
        String sentiment, // Extreme Greed, Greed, Neutral, Fear, Extreme Fear
        String nasdaqImpact,
        String currencyImpact,
        String vixImpact,
        String riskStatus, // RISK_ON, RISK_OFF
        String summary) {
    /**
     * Risk-On 상태인지 확인
     */
    public boolean isRiskOn() {
        return "RISK_ON".equals(riskStatus);
    }

    /**
     * Risk-Off 상태인지 확인 (신규 진입 차단)
     */
    public boolean isRiskOff() {
        return "RISK_OFF".equals(riskStatus);
    }

    /**
     * 공포 상태인지 확인
     */
    public boolean isFear() {
        return sentiment != null && sentiment.contains("Fear");
    }

    /**
     * 탐욕 상태인지 확인
     */
    public boolean isGreed() {
        return sentiment != null && sentiment.contains("Greed");
    }
}
