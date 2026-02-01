package com.kairos.trading.domain.execution.dto;

import java.util.List;

/**
 * 미체결 주문 조회 응답 DTO (ka10075).
 * Aegis 에이전트가 미체결 관리에 사용.
 */
public record UnfilledOrderResponse(
        List<UnfilledOrder> orders) {
    /**
     * 미체결 주문 상세.
     */
    public record UnfilledOrder(
            String orderNo, // 주문번호 (ord_no)
            String orderTime, // 주문시간 (ord_tm)
            String stockCode, // 종목코드 (stk_cd)
            String stockName, // 종목명 (stk_nm)
            String orderType, // 주문구분 (ord_tp) - 매수/매도
            int orderQuantity, // 주문수량 (ord_qty)
            int orderPrice, // 주문가격 (ord_pric)
            int filledQuantity, // 체결수량 (cntr_qty)
            int unfilledQuantity, // 미체결수량 (uncntr_qty)
            int currentPrice, // 현재가 (cur_prc)
            String orderStatus // 주문상태 (ord_stts)
    ) {
        /**
         * 전량 미체결 여부.
         */
        public boolean isFullyUnfilled() {
            return filledQuantity == 0;
        }

        /**
         * 부분 체결 여부.
         */
        public boolean isPartiallyFilled() {
            return filledQuantity > 0 && unfilledQuantity > 0;
        }
    }

    /**
     * 미체결 주문 수.
     */
    public int getUnfilledCount() {
        return orders != null ? orders.size() : 0;
    }
}
