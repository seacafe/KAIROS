package com.kairos.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * KAIROS: 7인 AI 에이전트 기반 하이브리드 트레이딩 시스템
 * 
 * Java 21 Virtual Threads + Spring Boot 3.4 기반의 고성능 동시성 아키텍처.
 * 자세한 설명은 docs/PROJECT-Specification.md 참조.
 */
@SpringBootApplication
public class KairosTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(KairosTradingApplication.class, args);
    }
}
