package com.kairos.trading.common.gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 전역 트래픽 제어 컴포넌트 (The Gatekeeper).
 * 
 * 모든 외부 API 호출은 이 컴포넌트를 통해서만 수행되어야 한다.
 * API별로 독립적인 Token Bucket을 운영하여 Rate Limit을 준수한다.
 * 
 * Virtual Thread의 park()를 활용하여 대기 시 OS 자원을 점유하지 않는다.
 * 
 * @see ApiType
 */
@Slf4j
@Component
public class ApiGatekeeper {

    private final Map<ApiType, Bucket> buckets = new EnumMap<>(ApiType.class);

    @PostConstruct
    public void initBuckets() {
        // Kiwoom: 4 req/sec (Strict - Leaky Bucket 방식으로 동작)
        // 실제로는 5 req/sec 제한이지만, 안전 마진을 두어 Ban 방지
        buckets.put(ApiType.KIWOOM, Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(4)
                        .refillGreedy(4, Duration.ofSeconds(1))
                        .build())
                .build());

        // Naver: 10 req/sec (일일 25,000건 Quota는 별도 관리 필요)
        buckets.put(ApiType.NAVER, Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofSeconds(1))
                        .build())
                .build());

        // Gemini: 1,000 req/min (Pay-as-you-go - Cost Safety Cap)
        buckets.put(ApiType.GEMINI, Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1000)
                        .refillGreedy(1000, Duration.ofMinutes(1))
                        .build())
                .build());

        log.info("ApiGatekeeper 초기화 완료 - {} 개의 API 버킷 생성", buckets.size());
    }

    /**
     * Rate Limit을 준수하며 외부 API를 호출한다.
     * 
     * 토큰이 부족하면 Virtual Thread가 park() 상태로 전환되어 대기한다.
     * 대기 중에도 OS 스레드 자원을 점유하지 않으므로 안전하다.
     * 
     * @param <T>     반환 타입
     * @param apiType 호출할 API 타입
     * @param action  실행할 작업 (Supplier)
     * @return 작업 실행 결과
     * @throws IllegalArgumentException apiType 또는 action이 null인 경우
     */
    public <T> T execute(ApiType apiType, Supplier<T> action) {
        Objects.requireNonNull(apiType, "ApiType은 null일 수 없습니다.");
        Objects.requireNonNull(action, "Action(Supplier)은 null일 수 없습니다.");

        var bucket = buckets.get(apiType);
        if (bucket == null) {
            throw new IllegalStateException("Unknown ApiType: " + apiType);
        }

        // 토큰을 소비하고 대기 (Blocking - Virtual Thread 환경에서 효율적)
        try {
            bucket.asBlocking().consume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rate Limiter 대기 중 인터럽트 발생", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] API 호출 실행 (남은 토큰: {})",
                    apiType, bucket.getAvailableTokens());
        }

        return action.get();
    }

    /**
     * 특정 API의 현재 사용 가능한 토큰 수를 반환한다.
     * 모니터링 및 디버깅 용도.
     */
    public long getAvailableTokens(ApiType apiType) {
        var bucket = buckets.get(apiType);
        return bucket != null ? bucket.getAvailableTokens() : 0;
    }
}
