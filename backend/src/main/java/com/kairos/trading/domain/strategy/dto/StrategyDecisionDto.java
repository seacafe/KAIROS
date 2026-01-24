package com.kairos.trading.domain.strategy.dto;

/**
 * 전략 의사결정 결과 DTO.
 * Nexus 에이전트가 반환하는 최종 의사결정.
 */
public record StrategyDecisionDto(
        String decision, // BUY, WATCH, REJECT, ALERT
        int finalScore, // 종합 점수 (0~100)
        String riskLevel, // HIGH, MEDIUM, LOW
        double positionSize, // 추천 투자 비중 (0.0~1.0)
        long targetPrice, // 추천 목표가
        long stopLossPrice, // 추천 손절가
        String reasoning, // 의사결정 근거
        String dissent // 반대 의견 (있을 경우)
) {
    /**
     * 매수 승인인지 확인
     */
    public boolean isBuyApproved() {
        return "BUY".equals(decision);
    }

    /**
     * Kill Switch 발동인지 확인
     */
    public boolean isAlert() {
        return "ALERT".equals(decision);
    }

    /**
     * 기각인지 확인
     */
    public boolean isRejected() {
        return "REJECT".equals(decision);
    }

    /**
     * 관망인지 확인
     */
    public boolean isWatch() {
        return "WATCH".equals(decision);
    }
}
