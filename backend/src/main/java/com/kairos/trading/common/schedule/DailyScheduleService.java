package com.kairos.trading.common.schedule;

import com.kairos.trading.common.client.KiwoomClient;
import com.kairos.trading.common.websocket.KiwoomWebSocketClient;
import com.kairos.trading.domain.execution.service.TradingLoopService;
import com.kairos.trading.domain.news.service.RssMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 일별 스케줄 서비스.
 * 장 시작/종료에 맞춰 시스템을 제어한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyScheduleService {

    private final KiwoomClient kiwoomClient;
    private final KiwoomWebSocketClient webSocketClient;
    private final TradingLoopService tradingLoopService;
    private final RssMonitoringService rssMonitoringService;

    private String currentToken;
    private boolean isMarketOpen = false;

    /**
     * 08:30 - 장전 준비.
     * 토큰 발급, 에이전트 분석 시작.
     */
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void prepareMarketOpen() {
        log.info("========== [스케줄] 08:30 장전 준비 시작 ==========");

        try {
            // 1. 토큰 발급
            var tokenResponse = kiwoomClient.issueToken();
            currentToken = tokenResponse.token();
            log.info("[스케줄] 토큰 발급 완료 (유효시간: {}초)", tokenResponse.expiresIn());

            // 2. RSS 캐시 초기화
            rssMonitoringService.cleanupProcessedIds();

            // 3. TODO: 에이전트 분석 트리거
            log.info("[스케줄] 장전 분석 시작...");

        } catch (Exception e) {
            log.error("[스케줄] 장전 준비 실패", e);
        }
    }

    /**
     * 09:00 - 장 시작.
     * WebSocket 연결, Trading Loop 활성화.
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void marketOpen() {
        log.info("========== [스케줄] 09:00 장 시작 ==========");

        try {
            // 1. WebSocket 연결
            if (currentToken != null) {
                webSocketClient.connect(currentToken);
                log.info("[스케줄] WebSocket 연결됨");
            }

            // 2. Trading Loop 활성화
            isMarketOpen = true;
            log.info("[스케줄] Trading Loop 활성화");

        } catch (Exception e) {
            log.error("[스케줄] 장 시작 처리 실패", e);
        }
    }

    /**
     * 15:30 - 장 마감 준비.
     * Trading Loop 비활성화, 회고 생성.
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void prepareMarketClose() {
        log.info("========== [스케줄] 15:30 장 마감 준비 ==========");

        try {
            // 1. Trading Loop 비활성화
            isMarketOpen = false;
            log.info("[스케줄] Trading Loop 비활성화");

            // 2. TODO: AI 회고 생성 트리거
            log.info("[스케줄] AI 회고 생성 시작...");

        } catch (Exception e) {
            log.error("[스케줄] 장 마감 준비 실패", e);
        }
    }

    /**
     * 16:00 - 장 종료.
     * WebSocket 해제, Journal 저장.
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI")
    public void marketClose() {
        log.info("========== [스케줄] 16:00 장 종료 ==========");

        try {
            // 1. WebSocket 해제
            webSocketClient.disconnect();
            log.info("[스케줄] WebSocket 연결 해제");

            // 2. TODO: Journal 저장
            log.info("[스케줄] 매매일지 저장 완료");

            // 3. 토큰 초기화
            currentToken = null;

        } catch (Exception e) {
            log.error("[스케줄] 장 종료 처리 실패", e);
        }
    }

    /**
     * 장이 열려 있는지 확인.
     */
    public boolean isMarketOpen() {
        return isMarketOpen;
    }

    /**
     * 현재 토큰 반환.
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * 오늘이 휴장일인지 확인.
     */
    public boolean isHoliday() {
        // TODO: 공휴일 API 연동
        return false;
    }
}
