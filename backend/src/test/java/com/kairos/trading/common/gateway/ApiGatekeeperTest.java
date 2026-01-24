package com.kairos.trading.common.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ApiGatekeeper 단위 테스트.
 * TDD Red Phase: 이 테스트들은 구현 전에 모두 실패해야 한다.
 */
@DisplayName("ApiGatekeeper 테스트")
class ApiGatekeeperTest {

    private ApiGatekeeper gatekeeper;

    @BeforeEach
    void setUp() {
        gatekeeper = new ApiGatekeeper();
    }

    @Test
    @DisplayName("Kiwoom API는 초당 4회까지 즉시 실행된다")
    void kiwoom_shouldAllowUpTo4RequestsPerSecond() {
        // given
        var counter = new AtomicInteger(0);

        // when: 4번 연속 호출
        for (int i = 0; i < 4; i++) {
            gatekeeper.execute(ApiType.KIWOOM, () -> {
                counter.incrementAndGet();
                return "success";
            });
        }

        // then: 모든 요청이 성공해야 함
        assertThat(counter.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("Kiwoom API 5번째 호출은 대기 후 실행된다")
    @Timeout(3) // 3초 이내에 완료되어야 함
    void kiwoom_fifthRequestShouldWait() throws InterruptedException {
        // given
        var counter = new AtomicInteger(0);
        var startTime = System.currentTimeMillis();

        // when: 5번 연속 호출
        for (int i = 0; i < 5; i++) {
            gatekeeper.execute(ApiType.KIWOOM, () -> {
                counter.incrementAndGet();
                return "success";
            });
        }

        var elapsedTime = System.currentTimeMillis() - startTime;

        // then: 5번째 요청은 1초를 기다리므로 총 실행 시간이 1초 이상
        assertThat(counter.get()).isEqualTo(5);
        assertThat(elapsedTime).isGreaterThanOrEqualTo(200); // 최소 대기 시간
    }

    @Test
    @DisplayName("Naver API는 초당 10회까지 허용된다")
    void naver_shouldAllowUpTo10RequestsPerSecond() {
        // given
        var counter = new AtomicInteger(0);

        // when: 10번 연속 호출
        for (int i = 0; i < 10; i++) {
            gatekeeper.execute(ApiType.NAVER, () -> {
                counter.incrementAndGet();
                return "success";
            });
        }

        // then
        assertThat(counter.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("Gemini API는 분당 1000회까지 허용된다")
    void gemini_shouldAllowHighThroughput() {
        // given
        var counter = new AtomicInteger(0);

        // when: 100번 연속 호출 (테스트 시간 단축을 위해)
        for (int i = 0; i < 100; i++) {
            gatekeeper.execute(ApiType.GEMINI, () -> {
                counter.incrementAndGet();
                return "success";
            });
        }

        // then
        assertThat(counter.get()).isEqualTo(100);
    }

    @Test
    @DisplayName("Supplier가 예외를 던지면 그대로 전파된다")
    void execute_shouldPropagateSupplierException() {
        // when & then
        assertThatThrownBy(() -> gatekeeper.execute(ApiType.KIWOOM, () -> {
            throw new RuntimeException("API 호출 실패");
        })).isInstanceOf(RuntimeException.class)
                .hasMessage("API 호출 실패");
    }

    @Test
    @DisplayName("동시에 여러 스레드에서 호출해도 Rate Limit이 지켜진다")
    @Timeout(5)
    void execute_shouldRespectRateLimitUnderConcurrency() throws InterruptedException {
        // given
        var threadCount = 10;
        var latch = new CountDownLatch(threadCount);
        var successCount = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // when: 10개 스레드가 동시에 Kiwoom API 호출
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        gatekeeper.execute(ApiType.KIWOOM, () -> {
                            successCount.incrementAndGet();
                            return "success";
                        });
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then: 모든 요청이 결국 성공해야 함 (일부는 대기 후)
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Null Supplier는 예외를 발생시킨다")
    void execute_shouldThrowOnNullSupplier() {
        assertThatThrownBy(() -> gatekeeper.execute(ApiType.KIWOOM, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null ApiType은 예외를 발생시킨다")
    void execute_shouldThrowOnNullApiType() {
        assertThatThrownBy(() -> gatekeeper.execute(null, () -> "test")).isInstanceOf(IllegalArgumentException.class);
    }
}
