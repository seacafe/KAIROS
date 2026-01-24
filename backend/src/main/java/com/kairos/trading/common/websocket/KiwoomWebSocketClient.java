package com.kairos.trading.common.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.common.event.ProgramTradeEvent;
import com.kairos.trading.common.event.TickDataEvent;
import com.kairos.trading.common.event.ViEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 키움증권 WebSocket 클라이언트.
 * 
 * 실시간 데이터 수신:
 * - 00: 체결가
 * - 0w: 프로그램 매매
 * - 1h: VI 발동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomWebSocketClient {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kiwoom.websocket.url:wss://api.kiwoom.com/websocket}")
    private String websocketUrl;

    private ReactorNettyWebSocketClient client;
    private Disposable connection;

    // 구독 중인 종목
    private final Set<String> subscribedStocks = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        client = new ReactorNettyWebSocketClient();
        log.info("KiwoomWebSocketClient 초기화 완료");
    }

    @PreDestroy
    public void cleanup() {
        disconnect();
    }

    /**
     * WebSocket 연결 시작.
     */
    public void connect(String token) {
        if (connection != null && !connection.isDisposed()) {
            log.warn("이미 WebSocket 연결이 활성화되어 있습니다.");
            return;
        }

        log.info("WebSocket 연결 시작: {}", websocketUrl);

        connection = client.execute(
                URI.create(websocketUrl + "?token=" + token),
                session -> session
                        .receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(this::handleMessage)
                        .doOnError(e -> log.error("WebSocket 에러", e))
                        .doOnComplete(() -> log.info("WebSocket 연결 종료"))
                        .then())
                .retryWhen(retry -> retry
                        .fixedDelay(5, Duration.ofSeconds(5))
                        .doBeforeRetry(signal -> log.warn("WebSocket 재연결 시도: {}", signal.totalRetries())))
                .subscribe();
    }

    /**
     * WebSocket 연결 종료.
     */
    public void disconnect() {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
            log.info("WebSocket 연결 해제됨");
        }
        subscribedStocks.clear();
    }

    /**
     * 종목 실시간 구독 등록.
     */
    public void subscribe(String stockCode) {
        subscribedStocks.add(stockCode);
        log.info("실시간 구독 등록: {}", stockCode);
        // TODO: 실제 구독 메시지 전송
    }

    /**
     * 종목 실시간 구독 해제.
     */
    public void unsubscribe(String stockCode) {
        subscribedStocks.remove(stockCode);
        log.info("실시간 구독 해제: {}", stockCode);
        // TODO: 실제 구독 해제 메시지 전송
    }

    /**
     * 메시지 핸들링.
     */
    private void handleMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String trCode = node.path("tr_cd").asText();

            switch (trCode) {
                case "00" -> handleTickData(node);
                case "0w" -> handleProgramTrade(node);
                case "1h" -> handleViEvent(node);
                default -> log.debug("알 수 없는 TR 코드: {}", trCode);
            }
        } catch (Exception e) {
            log.error("메시지 파싱 실패: {}", message, e);
        }
    }

    /**
     * 체결가 데이터 처리 (00).
     */
    private void handleTickData(JsonNode node) {
        var event = new TickDataEvent(
                this,
                node.path("stk_cd").asText(),
                node.path("cur_prc").asLong(),
                node.path("trd_vol").asLong(),
                node.path("acc_vol").asLong(),
                node.path("chg_rate").asDouble());

        eventPublisher.publishEvent(event);
        log.trace("체결: {} @ {} ({}%)", event.getStockCode(), event.getPrice(), event.getChangeRate());
    }

    /**
     * 프로그램 매매 데이터 처리 (0w).
     */
    private void handleProgramTrade(JsonNode node) {
        var event = new ProgramTradeEvent(
                this,
                node.path("stk_cd").asText(),
                node.path("pgm_buy").asLong(),
                node.path("pgm_sell").asLong());

        eventPublisher.publishEvent(event);

        if (event.isDistributionPattern()) {
            log.warn("⚠️ 프로그램 순매도 급증: {} ({}억)",
                    event.getStockCode(), event.getProgramNet() / 100_000_000);
        }
    }

    /**
     * VI 발동 처리 (1h).
     */
    private void handleViEvent(JsonNode node) {
        var event = new ViEvent(
                this,
                node.path("stk_cd").asText(),
                node.path("stk_nm").asText(),
                node.path("vi_tp").asText(),
                node.path("trig_prc").asLong());

        eventPublisher.publishEvent(event);
        log.warn("🚨 VI 발동: {} ({}) @ {}", event.getStockName(), event.getViType(), event.getTriggerPrice());
    }

    /**
     * 구독 중인 종목 수 반환.
     */
    public int getSubscribedCount() {
        return subscribedStocks.size();
    }

    /**
     * 연결 상태 확인.
     */
    public boolean isConnected() {
        return connection != null && !connection.isDisposed();
    }
}
