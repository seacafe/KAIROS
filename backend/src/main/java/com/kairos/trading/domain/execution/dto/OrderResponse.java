package com.kairos.trading.domain.execution.dto;

/**
 * 주식 주문 응답 DTO (kt10000~kt10003 공통).
 * Aegis 에이전트가 주문 결과 확인 시 사용.
 */
public record OrderResponse(
        String orderNo, // 주문번호 (ord_no)
        int returnCode, // 결과코드 (return_code) - 0: 정상
        String returnMsg, // 결과메시지 (return_msg)
        String originalOrderNo // 원주문번호 (정정/취소 시)
) {
    /**
     * 주문 성공 여부.
     */
    public boolean isSuccess() {
        return returnCode == 0;
    }
}
