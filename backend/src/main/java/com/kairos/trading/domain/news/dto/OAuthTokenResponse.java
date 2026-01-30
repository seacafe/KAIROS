package com.kairos.trading.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthTokenResponse(
        @JsonProperty("access_token") String accessToken, // Kiwoom returns access_token usually, but mock said 'token'?
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn) {
    // Alias for compatibility if needed, or fix mock
    public String token() {
        return accessToken;
    }
}
