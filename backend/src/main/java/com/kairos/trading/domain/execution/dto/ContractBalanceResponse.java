package com.kairos.trading.domain.execution.dto;

import java.util.List;

/**
 * 체결 잔고 응답 DTO (kt00005).
 * Aegis 에이전트가 보유 종목 및 수익률 확인에 사용.
 */
public record ContractBalanceResponse(
        long deposit, // 예수금 (entr)
        long depositD1, // 예수금D+1 (entr_d1)
        long depositD2, // 예수금D+2 (entr_d2)
        long withdrawableAmount, // 출금가능금액 (pymn_alow_amt)
        long orderableCash, // 주문가능현금 (ord_alowa)
        long totalPurchaseAmount, // 주식매수총액 (stk_buy_tot_amt)
        long totalEvaluationAmount, // 평가금액합계 (evlt_amt_tot)
        long totalProfitLoss, // 총손익합계 (tot_pl_tot)
        double totalProfitLossRate, // 총손익률 (tot_pl_rt)
        List<ContractStock> stocks // 종목별 체결잔고
) {
    /**
     * 종목별 체결 잔고.
     */
    public record ContractStock(
            String creditType, // 신용구분 (crd_tp)
            String loanDate, // 대출일 (loan_dt)
            String expiryDate, // 만기일 (expr_dt)
            String stockCode, // 종목코드 (stk_cd)
            String stockName, // 종목명 (stk_nm)
            int settleBalance, // 결제잔고 (setl_remn)
            int currentBalance, // 현재잔고 (cur_qty)
            int currentPrice, // 현재가 (cur_prc)
            int buyPrice, // 매입단가 (buy_uv)
            long purchaseAmount, // 매입금액 (pur_amt)
            long evaluationAmount, // 평가금액 (evlt_amt)
            long profitLoss, // 평가손익 (evltv_prft)
            double profitLossRate // 손익률 (pl_rt)
    ) {
    }
}
