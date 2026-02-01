package com.kairos.trading.common.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 메시지 발송 서비스.
 * Frontend로 실시간 알림 및 거래 업데이트 전송.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    // Topics
    private static final String TOPIC_ALERT = "/topic/alert";
    private static final String TOPIC_TRADE = "/topic/trade";
    private static final String TOPIC_PRICE = "/topic/price";

    /**
     * 알림 메시지 전송.
     * 
     * @param type    알림 유형 (NEWS, RISK, ORDER, SYSTEM)
     * @param title   알림 제목
     * @param message 알림 내용
     * @param level   심각도 (INFO, WARNING, CRITICAL)
     */
    public void sendAlert(String type, String title, String message, String level) {
        var payload = Map.of(
                "type", type,
                "title", title,
                "message", message,
                "level", level,
                "timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(TOPIC_ALERT, payload);
        log.info("[WebSocket] Alert 전송: {} - {}", type, title);
    }

    /**
     * 거래 업데이트 전송.
     * 
     * @param stockCode 종목코드
     * @param action    액션 (BUY, SELL)
     * @param status    상태 (SUBMITTED, FILLED, REJECTED)
     * @param details   추가 정보
     */
    public void sendTradeUpdate(String stockCode, String action, String status, Map<String, Object> details) {
        var payload = Map.of(
                "stockCode", stockCode,
                "action", action,
                "status", status,
                "details", details,
                "timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(TOPIC_TRADE, payload);
        log.info("[WebSocket] Trade 전송: {} {} - {}", action, stockCode, status);
    }

    /**
     * 실시간 가격 업데이트 전송.
     * 
     * @param stockCode 종목코드
     * @param price     현재가
     * @param change    등락률
     */
    public void sendPriceUpdate(String stockCode, double price, double change) {
        var payload = Map.of(
                "stockCode", stockCode,
                "price", price,
                "change", change,
                "timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(TOPIC_PRICE, payload);
    }

    /**
     * Kill Switch 긴급 알림 전송.
     * 
     * @param reason    발동 사유
     * @param stockCode 관련 종목 (optional)
     */
    public void sendKillSwitchAlert(String reason, String stockCode) {
        var payload = Map.of(
                "type", "KILL_SWITCH",
                "title", "🚨 긴급 매도 발동",
                "message", reason,
                "stockCode", stockCode != null ? stockCode : "",
                "level", "CRITICAL",
                "timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(TOPIC_ALERT, payload);
        log.warn("[WebSocket] Kill Switch Alert 전송: {} - {}", stockCode, reason);
    }
}
