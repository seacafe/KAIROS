package com.kairos.trading.domain.execution.dto;

/**
 * 슬리피지 분석 결과 DTO.
 * Aegis Review AI 클라이언트가 반환하는 분석 결과.
 */
public record SlippageAnalysisDto(
        long tradeId,
        long orderPrice,
        long filledPrice,
        double slippageRate,
        String cause, // ORDER_DELAY, LIQUIDITY_GAP, PRICE_MOVEMENT
        String causeDetail,
        int timeOffsetMs, // 시간 보정값 (밀리초)
        int tickOffset, // 호가 단위 보정
        String suggestion) {
    /**
     * 주문 지연이 원인인지 확인
     */
    public boolean isOrderDelay() {
        return "ORDER_DELAY".equals(cause);
    }

    /**
     * 유동성 부족이 원인인지 확인
     */
    public boolean isLiquidityGap() {
        return "LIQUIDITY_GAP".equals(cause);
    }

    /**
     * 슬리피지가 심각한지 확인 (0.5% 이상)
     */
    public boolean isSevere() {
        return slippageRate >= 0.5;
    }
}
