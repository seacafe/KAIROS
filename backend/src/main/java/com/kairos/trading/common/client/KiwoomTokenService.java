package com.kairos.trading.common.client;

import com.kairos.trading.domain.news.dto.OAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 키움 API 토큰 관리 서비스.
 * 
 * 토큰의 발급, 캐싱, 자동 갱신을 담당한다.
 * Thread-safe하게 동작하며, 토큰 만료 시 자동으로 재발급한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KiwoomTokenService {

    private final KiwoomClient kiwoomClient;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // 만료 5분 전에 미리 갱신
    private static final long REFRESH_MARGIN_SECONDS = 300;

    /**
     * 유효한 액세스 토큰을 반환한다.
     * 캐싱된 토큰이 없거나 만료 임박 시 자동으로 재발급한다.
     */
    public String getValidToken() {
        if (isTokenValid()) {
            return cachedToken;
        }

        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return cachedToken;
            }

            log.info("[TokenService] 토큰 발급/갱신 시작");
            OAuthTokenResponse response = kiwoomClient.issueToken();

            this.cachedToken = response.token();
            this.tokenExpiry = Instant.now().plusSeconds(response.expiresIn());

            log.info("[TokenService] 토큰 발급 완료 (만료: {})", tokenExpiry);
            return cachedToken;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 토큰이 유효한지 확인한다.
     */
    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiry == null) {
            return false;
        }
        // 만료 5분 전이면 갱신 필요
        return Instant.now().plusSeconds(REFRESH_MARGIN_SECONDS).isBefore(tokenExpiry);
    }

    /**
     * 토큰을 강제로 무효화한다.
     * API 인증 에러 발생 시 호출하여 재발급을 유도한다.
     */
    public void invalidateToken() {
        tokenLock.lock();
        try {
            log.warn("[TokenService] 토큰 강제 무효화");
            this.cachedToken = null;
            this.tokenExpiry = null;
        } finally {
            tokenLock.unlock();
        }
    }
}
