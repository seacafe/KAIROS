package com.kairos.trading.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC(Mapped Diagnostic Context) 로깅 필터.
 * 모든 요청에 requestId와 threadId를 로그에 포함시킨다.
 * 
 * backendrule_kairos.md §1.4 준수
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String THREAD_ID = "threadId";
    private static final String THREAD_NAME = "threadName";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            // MDC에 요청 추적 정보 설정
            MDC.put(REQUEST_ID, generateRequestId());
            MDC.put(THREAD_ID, String.valueOf(Thread.currentThread().threadId()));
            MDC.put(THREAD_NAME, Thread.currentThread().getName());

            filterChain.doFilter(request, response);
        } finally {
            // 요청 완료 후 MDC 정리
            MDC.clear();
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
