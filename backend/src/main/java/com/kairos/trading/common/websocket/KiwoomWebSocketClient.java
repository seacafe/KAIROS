package com.kairos.trading.common.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kairos.trading.common.event.BalanceUpdateEvent;
import com.kairos.trading.common.event.OrderBookEvent;
import com.kairos.trading.common.event.ProgramTradeEvent;
import reactor.util.retry.Retry;
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
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 키움증권 WebSocket 클라이언트.
 * 
 * 실시간 데이터 수신:
 * - 00: 체결가
 * - 0A: 주식 기세
 * - 0w: 프로그램 매매
 * - 1h: VI 발동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomWebSocketClient {

    /**
     * 실시간 데이터 타입 상수.
     */
    public static class RealType {
        public static final String TICK_DATA = "00"; // 주식 체결
        public static final String BALANCE = "04"; // 잔고
        public static final String STOCK_QUOTE = "0A"; // 주식 기세
        public static final String STOCK_TRADE = "0B"; // 주식 체결 상세
        public static final String ORDER_BOOK = "0D"; // 주식 호가잔량
        public static final String PROGRAM_TRADE = "0w"; // 프로그램 매매
        public static final String VI_EVENT = "1h"; // VI 발동/해제
    }

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kiwoom.websocket.url:wss://openapi.koreainvestment.com:21000}")
    private String websocketUrl;

    private ReactorNettyWebSocketClient client;
    private Disposable connection;

    // 구독 중인 종목
    private final Set<String> subscribedStocks = ConcurrentHashMap.newKeySet();

    // 메시지 전송을 위한 Sink (양방향 통신)
    private Sinks.Many<String> outboundSink;

    // 기본 그룹번호
    private static final String DEFAULT_GROUP_NO = "1";

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

        // 메시지 전송용 Sink 초기화
        outboundSink = Sinks.many().unicast().onBackpressureBuffer();

        connection = client.execute(
                URI.create(websocketUrl + "?token=" + token),
                session -> {
                    // 수신 스트림
                    var inbound = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(this::handleMessage)
                            .doOnError(e -> log.error("WebSocket 수신 에러", e));

                    // 송신 스트림
                    var outbound = session.send(
                            outboundSink.asFlux()
                                    .map(session::textMessage)
                                    .doOnNext(msg -> log.debug("WebSocket 송신: {}", msg.getPayloadAsText())));

                    // 양방향 병합
                    return Mono.zip(inbound.then(), outbound).then();
                })
                .retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(5))
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
     * 
     * @param stockCode 종목코드
     * @param realTypes 실시간 타입 목록 (기본: 체결가)
     */
    public void subscribe(String stockCode, List<String> realTypes) {
        if (!isConnected()) {
            log.warn("WebSocket 미연결 상태. 구독 불가: {}", stockCode);
            return;
        }

        subscribedStocks.add(stockCode);

        String message = buildSubscribeMessage(
                List.of(stockCode),
                realTypes.isEmpty() ? List.of(RealType.TICK_DATA) : realTypes,
                DEFAULT_GROUP_NO,
                true);

        sendMessage(message);
        log.info("실시간 구독 등록: {} (types: {})", stockCode, realTypes);
    }

    /**
     * 종목 실시간 구독 등록 (체결가 기본).
     */
    public void subscribe(String stockCode) {
        subscribe(stockCode, List.of(RealType.TICK_DATA));
    }

    /**
     * 종목 실시간 구독 해제.
     */
    public void unsubscribe(String stockCode) {
        if (!isConnected()) {
            log.warn("WebSocket 미연결 상태. 해제 불가: {}", stockCode);
            return;
        }

        subscribedStocks.remove(stockCode);

        String message = buildUnsubscribeMessage(
                List.of(stockCode),
                List.of(RealType.TICK_DATA, RealType.PROGRAM_TRADE),
                DEFAULT_GROUP_NO);

        sendMessage(message);
        log.info("실시간 구독 해제: {}", stockCode);
    }

    /**
     * 메시지 전송.
     */
    private void sendMessage(String message) {
        if (outboundSink != null) {
            outboundSink.tryEmitNext(message);
        } else {
            log.error("outboundSink가 null입니다. WebSocket이 연결되지 않았습니다.");
        }
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
                case "04" -> handleBalance(node);
                case "0D" -> handleOrderBook(node);
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
     * 실시간 잔고 처리 (04).
     * 체결 후 잔고 자동 동기화.
     */
    private void handleBalance(JsonNode node) {
        var event = new BalanceUpdateEvent(
                this,
                node.path("acnt_no").asText(),
                node.path("stk_cd").asText(),
                node.path("stk_nm").asText(),
                node.path("hold_qty").asInt(),
                node.path("avg_prc").asLong(),
                node.path("cur_prc").asLong(),
                node.path("eval_amt").asLong(),
                node.path("pnl_amt").asLong(),
                node.path("pnl_rt").asDouble());

        eventPublisher.publishEvent(event);
        log.info("💰 잔고 업데이트: {} {} 주 @ {} (손익: {})",
                event.getStockName(), event.getHoldQty(), event.getCurrentPrice(), event.getPnlAmount());
    }

    /**
     * 실시간 호가잔량 처리 (0D).
     * Vector 에이전트가 호가창 변동 감지에 활용.
     */
    private void handleOrderBook(JsonNode node) {
        var event = new OrderBookEvent(
                this,
                node.path("stk_cd").asText(),
                node.path("stk_nm").asText(),
                node.path("sell_hoga1").asLong(),
                node.path("sell_qty1").asLong(),
                node.path("buy_hoga1").asLong(),
                node.path("buy_qty1").asLong(),
                node.path("tot_sell_qty").asLong(),
                node.path("tot_buy_qty").asLong());

        eventPublisher.publishEvent(event);
        log.debug("📊 호가 변동: {} 매도1: {}@{} 매수1: {}@{}",
                event.getStockCode(), event.getAskPrice1(), event.getAskQty1(),
                event.getBidPrice1(), event.getBidQty1());
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

    /**
     * 구독 요청 메시지 생성 (REG).
     * 
     * @param stockCodes   종목코드 리스트
     * @param realTypes    실시간 타입 리스트 (00, 0A, 0w, 1h)
     * @param groupNo      그룹번호 (1~4)
     * @param keepExisting true: 기존 구독 유지, false: 기존 제거 후 등록
     * @return JSON 형식의 구독 요청 메시지
     */
    public String buildSubscribeMessage(List<String> stockCodes, List<String> realTypes,
            String groupNo, boolean keepExisting) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("trnm", "REG");
        root.put("grp_no", groupNo);
        root.put("refresh", keepExisting ? "1" : "0");

        ArrayNode dataArray = root.putArray("data");
        ObjectNode dataItem = dataArray.addObject();

        ArrayNode itemArray = dataItem.putArray("item");
        stockCodes.forEach(itemArray::add);

        ArrayNode typeArray = dataItem.putArray("type");
        realTypes.forEach(typeArray::add);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("구독 메시지 생성 실패", e);
            throw new RuntimeException("구독 메시지 생성 실패", e);
        }
    }

    /**
     * 구독 해제 요청 메시지 생성 (REMOVE).
     * 
     * @param stockCodes 종목코드 리스트
     * @param realTypes  실시간 타입 리스트
     * @param groupNo    그룹번호
     * @return JSON 형식의 해제 요청 메시지
     */
    public String buildUnsubscribeMessage(List<String> stockCodes, List<String> realTypes,
            String groupNo) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("trnm", "REMOVE");
        root.put("grp_no", groupNo);

        ArrayNode dataArray = root.putArray("data");
        ObjectNode dataItem = dataArray.addObject();

        ArrayNode itemArray = dataItem.putArray("item");
        stockCodes.forEach(itemArray::add);

        ArrayNode typeArray = dataItem.putArray("type");
        realTypes.forEach(typeArray::add);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("해제 메시지 생성 실패", e);
            throw new RuntimeException("해제 메시지 생성 실패", e);
        }
    }
}
