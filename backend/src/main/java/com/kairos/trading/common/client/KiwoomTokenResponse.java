package com.kairos.trading.common.client;

/**
 * 키움 API 토큰 응답 DTO (au10001).
 */
public record KiwoomTokenResponse(
        String token,
        String tokenType,
        long expiresIn) {
}
