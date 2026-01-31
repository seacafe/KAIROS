package com.kairos.trading.domain.account.dto;

import java.util.List;

/**
 * 체결 잔고 응답 DTO (kt00005).
 * 
 * 예수금 상세 및 종목별 체결잔고 포함.
 */
public record ExecutionBalanceResponse(
        // 예수금 상세
        long deposit, // 예수금
        long depositD1, // 예수금 D+1
        long depositD2, // 예수금 D+2
        long withdrawableAmount, // 출금가능금액
        long orderableAmount, // 주문가능현금
        long totalPurchaseAmount, // 주식매수총액
        long totalEvalAmount, // 평가금액합계
        long totalProfitLoss, // 총손익합계
        double totalProfitLossRate, // 총손익률

        // 체결잔고 리스트
        List<ExecutionStock> executionStocks) {
    /**
     * 종목별 체결잔고 DTO.
     */
    public record ExecutionStock(
            String stockCode, // 종목코드
            String stockName, // 종목명
            int settledBalance, // 결제잔고
            int currentBalance, // 현재잔고
            int currentPrice, // 현재가
            int buyPrice, // 매입단가
            long purchaseAmount, // 매입금액
            long evalAmount, // 평가금액
            long evalProfit, // 평가손익
            double profitLossRate, // 손익률
            String creditType, // 신용구분
            String loanDate, // 대출일
            String expiryDate // 만기일
    ) {
        /**
         * 현금 매수 여부.
         */
        public boolean isCashPurchase() {
            return "현금".equals(creditType) || creditType == null || creditType.isEmpty();
        }

        /**
         * 만기 임박 여부 (대출 종목).
         */
        public boolean isExpiryNear() {
            // 간단 구현: 만기일이 있으면 체크 필요
            return expiryDate != null && !expiryDate.isEmpty();
        }
    }

    /**
     * 매수 가능 여부.
     */
    public boolean canBuy(int amount) {
        return orderableAmount >= amount;
    }

    /**
     * 체결잔고 종목 수.
     */
    public int executionCount() {
        return executionStocks != null ? executionStocks.size() : 0;
    }
}
