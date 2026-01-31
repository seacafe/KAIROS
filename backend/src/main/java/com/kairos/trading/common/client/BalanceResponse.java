package com.kairos.trading.common.client;

/**
 * 예수금 조회 응답 DTO (kt00004).
 */
public record BalanceResponse(
        long availableAmount, // 주문가능현금 (ord_psbl_cash)
        long totalEvalAmount, // 총평가금액 (tot_evlu_amt)
        long purchaseAmount, // 매입금액합계 (pchs_amt_smtl)
        long profitLossAmount, // 평가손익합계 (evlu_pfls_smtl)
        double profitLossRate // 평가손익률 (evlu_pfls_rt)
) {
    /**
     * 특정 금액으로 매수 가능한지 확인.
     */
    public boolean canAfford(long amount) {
        return availableAmount >= amount;
    }
}
