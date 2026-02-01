package com.kairos.trading.domain.execution.dto;

/**
 * 주식 주문 요청 DTO (kt10000~kt10003 공통).
 * Aegis 에이전트가 주문 실행 시 사용.
 */
public record OrderRequest(
        String stockCode, // 종목코드 (stk_cd)
        int quantity, // 주문수량 (ord_qty)
        int price, // 주문단가 (ord_uv) - 시장가일 경우 0
        TradeType tradeType, // 거래구분 (trde_tp)
        String originalOrderNo // 원주문번호 (정정/취소 시 필수)
) {
    /**
     * 거래 구분.
     */
    public enum TradeType {
        LIMIT("00"), // 지정가
        MARKET("03"), // 시장가
        CONDITIONAL("05"), // 조건부지정가
        BEST("06"), // 최유리지정가
        FIRST("07"), // 최우선지정가
        AFTER_MARKET("10"), // 장전시간외
        CLOSING("16"), // 장마감시간외
        SINGLE("26"); // 시간외단일가

        private final String code;

        TradeType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * 매수 주문 생성 (시장가).
     */
    public static OrderRequest marketBuy(String stockCode, int quantity) {
        return new OrderRequest(stockCode, quantity, 0, TradeType.MARKET, null);
    }

    /**
     * 매수 주문 생성 (지정가).
     */
    public static OrderRequest limitBuy(String stockCode, int quantity, int price) {
        return new OrderRequest(stockCode, quantity, price, TradeType.LIMIT, null);
    }

    /**
     * 취소 주문 생성.
     */
    public static OrderRequest cancel(String stockCode, int quantity, String originalOrderNo) {
        return new OrderRequest(stockCode, quantity, 0, null, originalOrderNo);
    }
}
