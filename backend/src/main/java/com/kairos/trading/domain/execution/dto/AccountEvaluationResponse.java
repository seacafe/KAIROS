package com.kairos.trading.domain.execution.dto;

import java.util.List;

/**
 * 계좌 평가 현황 응답 DTO (kt00004).
 * Aegis 에이전트가 자금 배분 및 리스크 관리에 사용.
 */
public record AccountEvaluationResponse(
        String accountNo, // 계좌번호 (acnt_nm)
        String branchName, // 지점명 (brch_nm)
        long deposit, // 예수금 (entr)
        long d2EstimatedDeposit, // D+2추정예수금 (d2_entra) - 주문가능액 기준
        long totalEvaluationAmount, // 유가잔고평가액 (tot_est_amt)
        long totalPurchaseAmount, // 총매입금액 (tot_pur_amt)
        long todayProfitLoss, // 당일투자손익 (tdy_lspft)
        double todayProfitLossRate, // 당일손익율 (tdy_lspft_rt)
        long totalProfitLoss, // 누적투자손익 (lspft)
        double totalProfitLossRate, // 누적손익율 (lspft_rt)
        List<StockHolding> holdings // 종목별 보유 현황
) {
    /**
     * 종목별 보유 현황.
     */
    public record StockHolding(
            String stockCode, // 종목코드 (stk_cd)
            String stockName, // 종목명 (stk_nm)
            int quantity, // 보유수량 (rmnd_qty)
            int avgPrice, // 평균단가 (avg_prc)
            int currentPrice, // 현재가 (cur_prc)
            long evaluationAmount, // 평가금액 (evlt_amt)
            long profitLoss, // 손익금액 (pl_amt)
            double profitLossRate // 손익율 (pl_rt)
    ) {
    }

    /**
     * 총 보유 종목 수.
     */
    public int getTotalHoldingCount() {
        return holdings != null ? holdings.size() : 0;
    }

    /**
     * 주문 가능 금액 (D+2 기준).
     */
    public long getOrderableAmount() {
        return d2EstimatedDeposit;
    }
}
