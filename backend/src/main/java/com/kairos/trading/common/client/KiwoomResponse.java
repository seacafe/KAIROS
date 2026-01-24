package com.kairos.trading.common.client;

/**
 * 키움 API 응답 DTO.
 * 공통 응답 구조를 정의한다.
 */
public record KiwoomResponse<T>(
        String resultCode, // "0000" = 성공
        String resultMessage,
        T data) {
    public boolean isSuccess() {
        return "0000".equals(resultCode);
    }
}
