package com.kairos.trading.domain.account.dto;

import java.util.List;

/**
 * 계좌 평가 현황 응답 DTO (kt00004).
 * 
 * 예수금, 평가금액, 손익 및 보유종목 리스트 포함.
 */
public record AccountEvaluationResponse(
        // 계좌 요약
        String accountName, // 계좌명
        long deposit, // 예수금
        long d2EstimatedDeposit, // D+2 추정예수금 (주문가능액)
        long totalEvalAmount, // 유가잔고평가액
        long totalPurchaseAmount, // 총매입금액
        long todayProfitLoss, // 당일투자손익
        double todayProfitLossRate, // 당일손익율
        long cumulativeProfitLoss, // 누적투자손익
        double cumulativeProfitLossRate, // 누적손익율

        // 보유종목 리스트
        List<HoldingStock> holdingStocks) {
    /**
     * 보유종목 DTO.
     */
    public record HoldingStock(
            String stockCode, // 종목코드
            String stockName, // 종목명
            int quantity, // 보유수량
            int averagePrice, // 평균단가
            int currentPrice, // 현재가
            long evalAmount, // 평가금액
            long profitLoss, // 손익금액
            double profitLossRate, // 손익율
            long purchaseAmount, // 매입금액
            int todayBuyQty, // 금일매수수량
            int todaySellQty // 금일매도수량
    ) {
        /**
         * 수익 종목 여부.
         */
        public boolean isProfitable() {
            return profitLoss > 0;
        }

        /**
         * 손절 필요 여부 (-5% 이하).
         */
        public boolean needsStopLoss() {
            return profitLossRate <= -5.0;
        }
    }

    /**
     * 전체 보유종목 수.
     */
    public int totalHoldingCount() {
        return holdingStocks != null ? holdingStocks.size() : 0;
    }

    /**
     * 수익 종목 수.
     */
    public long profitableCount() {
        return holdingStocks.stream().filter(HoldingStock::isProfitable).count();
    }

    /**
     * 손절 필요 종목 리스트.
     */
    public List<HoldingStock> stopLossTargets() {
        return holdingStocks.stream().filter(HoldingStock::needsStopLoss).toList();
    }
}
