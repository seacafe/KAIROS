package com.kairos.trading.domain.execution.dto;

import java.util.List;

/**
 * 계좌별주문체결내역상세요청(kt00007) 응답 DTO.
 * Aegis 에이전트가 당일 체결 내역 상세 조회에 활용.
 */
public record OrderExecutionDetailResponse(
        String continuationFlag, // 연속조회여부 (cont_yn)
        String nextKey, // 다음조회키 (next_key)
        List<OrderExecution> executions // 주문체결내역 (acnt_ord_cntr_prps_dtl)
) {
    /**
     * 주문체결 상세 항목.
     */
    public record OrderExecution(
            String orderNo, // 주문번호 (ord_no)
            String stockCode, // 종목코드 (stk_cd)
            String stockName, // 종목명 (stk_nm)
            String orderType, // 주문구분 (ord_tp)
            String tradeType, // 매매구분 (trde_tp)
            int orderQty, // 주문수량 (ord_qty)
            int orderPrice, // 주문단가 (ord_uv)
            int contractQty, // 체결수량 (cntr_qty)
            int contractPrice, // 체결단가 (cntr_uv)
            int unfilledQty, // 미체결수량 (uncntr_qty)
            String orderTime, // 주문시간 (ord_tm)
            String contractTime, // 체결시간 (cntr_tm)
            String orderStatus, // 주문상태 (ord_stts)
            String originalOrderNo, // 원주문번호 (orig_ord_no)
            long contractAmount, // 체결금액 (cntr_amt)
            String currencyOrderType, // 통화주문구분 (curncy_ord_tp)
            String domesticStockType // 국내거래소구분 (dmst_stex_tp)
    ) {
    }
}
