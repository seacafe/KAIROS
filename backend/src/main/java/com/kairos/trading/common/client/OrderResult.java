package com.kairos.trading.common.client;

/**
 * 주문 결과 응답 DTO (kt10000/kt10001).
 */
public record OrderResult(
        String orderId, // 주문번호 (ord_no)
        String orderTime, // 주문시각 (ord_tmd)
        String resultCode, // 결과코드 (rt_cd) - "0": 성공
        String messageCode, // 메시지코드 (msg_cd)
        String message // 메시지 (msg1)
) {
    /**
     * 주문 성공 여부 확인.
     */
    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    /**
     * 실패 결과 생성.
     */
    public static OrderResult failure(String messageCode, String message) {
        return new OrderResult(null, null, "-1", messageCode, message);
    }
}
