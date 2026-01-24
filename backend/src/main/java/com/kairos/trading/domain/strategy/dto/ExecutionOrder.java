package com.kairos.trading.domain.strategy.dto;

import java.math.BigDecimal;

/**
 * 매매 실행 지시 DTO.
 * Nexus가 생성하여 Aegis에게 전달한다.
 */
public record ExecutionOrder(
        String stockCode,
        String stockName,
        String action, // BUY, SELL
        int quantity, // 수량
        BigDecimal entryPrice, // 진입가
        BigDecimal targetPrice, // 목표가
        BigDecimal stopLossPrice, // 손절가
        String riskLevel, // HIGH, MEDIUM, LOW
        int priority, // 0=Kill Switch, 1=익절, 2=신규매수
        String reason // 실행 사유
) {
    /**
     * Kill Switch 매도 주문 생성
     */
    public static ExecutionOrder killSwitchSell(String stockCode, String stockName, int quantity, String reason) {
        return new ExecutionOrder(
                stockCode, stockName, "SELL",
                quantity, null, null, null,
                "HIGH", 0, reason);
    }

    /**
     * 익절 매도 주문 생성
     */
    public static ExecutionOrder profitTake(String stockCode, String stockName, int quantity, BigDecimal price) {
        return new ExecutionOrder(
                stockCode, stockName, "SELL",
                quantity, price, null, null,
                "MEDIUM", 1, "목표가 도달 익절");
    }

    /**
     * 신규 매수 주문 생성
     */
    public static ExecutionOrder newBuy(
            String stockCode, String stockName, int quantity,
            BigDecimal entryPrice, BigDecimal targetPrice, BigDecimal stopLossPrice,
            String riskLevel, String reason) {
        return new ExecutionOrder(
                stockCode, stockName, "BUY",
                quantity, entryPrice, targetPrice, stopLossPrice,
                riskLevel, 2, reason);
    }

    /**
     * Kill Switch 주문인지 확인
     */
    public boolean isKillSwitch() {
        return priority == 0;
    }
}
