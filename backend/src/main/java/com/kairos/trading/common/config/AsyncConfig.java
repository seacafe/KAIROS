package com.kairos.trading.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 및 스케줄링 설정.
 * 
 * Virtual Thread를 활용한 비동기 처리 및 RSS 폴링 스케줄러 활성화.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 이벤트 리스너용 비동기 Executor.
     * Virtual Thread 활용.
     */
    @Bean("eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.setVirtualThreads(true); // Virtual Thread 활용
        executor.initialize();
        return executor;
    }

    /**
     * RSS 폴링용 스케줄러 Executor.
     */
    @Bean("schedulerExecutor")
    public Executor schedulerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("scheduler-");
        executor.setVirtualThreads(true);
        executor.initialize();
        return executor;
    }
}
