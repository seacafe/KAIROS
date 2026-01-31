package com.kairos.trading.common.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KiwoomWebSocketClient 테스트.
 * 
 * TDD Red Step: 구독/해제 메시지 생성 로직 검증.
 */
@ExtendWith(MockitoExtension.class)
class KiwoomWebSocketClientTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private KiwoomWebSocketClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        client = new KiwoomWebSocketClient(eventPublisher);
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("구독 메시지 생성 테스트")
    class SubscribeMessageTest {

        @Test
        @DisplayName("단일 종목 체결(00) 구독 메시지 생성")
        void buildSubscribeMessage_singleStock_tickData() throws Exception {
            // Given
            List<String> stockCodes = List.of("005930");
            List<String> realTypes = List.of("00");

            // When
            String message = client.buildSubscribeMessage(stockCodes, realTypes, "1", true);

            // Then
            JsonNode json = objectMapper.readTree(message);
            assertThat(json.path("trnm").asText()).isEqualTo("REG");
            assertThat(json.path("grp_no").asText()).isEqualTo("1");
            assertThat(json.path("refresh").asText()).isEqualTo("1");

            JsonNode data = json.path("data").get(0);
            assertThat(data.path("item").get(0).asText()).isEqualTo("005930");
            assertThat(data.path("type").get(0).asText()).isEqualTo("00");
        }

        @Test
        @DisplayName("다중 종목 다중 타입 구독 메시지 생성")
        void buildSubscribeMessage_multipleStocks_multipleTypes() throws Exception {
            // Given
            List<String> stockCodes = List.of("005930", "000660", "035720");
            List<String> realTypes = List.of("00", "0w", "1h");

            // When
            String message = client.buildSubscribeMessage(stockCodes, realTypes, "2", false);

            // Then
            JsonNode json = objectMapper.readTree(message);
            assertThat(json.path("trnm").asText()).isEqualTo("REG");
            assertThat(json.path("grp_no").asText()).isEqualTo("2");
            assertThat(json.path("refresh").asText()).isEqualTo("0");

            JsonNode data = json.path("data").get(0);
            assertThat(data.path("item")).hasSize(3);
            assertThat(data.path("type")).hasSize(3);
        }

        @Test
        @DisplayName("VI 발동(1h) 전체 구독 - 종목코드 빈 배열")
        void buildSubscribeMessage_viEvent_emptyStockCode() throws Exception {
            // Given (VI 전체 구독은 종목코드 없이)
            List<String> stockCodes = List.of("");
            List<String> realTypes = List.of("1h");

            // When
            String message = client.buildSubscribeMessage(stockCodes, realTypes, "1", true);

            // Then
            JsonNode json = objectMapper.readTree(message);
            JsonNode data = json.path("data").get(0);
            assertThat(data.path("item").get(0).asText()).isEqualTo("");
            assertThat(data.path("type").get(0).asText()).isEqualTo("1h");
        }
    }

    @Nested
    @DisplayName("해제 메시지 생성 테스트")
    class UnsubscribeMessageTest {

        @Test
        @DisplayName("종목 구독 해제 메시지 생성")
        void buildUnsubscribeMessage_singleStock() throws Exception {
            // Given
            List<String> stockCodes = List.of("005930");
            List<String> realTypes = List.of("00");

            // When
            String message = client.buildUnsubscribeMessage(stockCodes, realTypes, "1");

            // Then
            JsonNode json = objectMapper.readTree(message);
            assertThat(json.path("trnm").asText()).isEqualTo("REMOVE");
            assertThat(json.path("grp_no").asText()).isEqualTo("1");
            assertThat(json.has("refresh")).isFalse(); // 해제시 refresh 불필요

            JsonNode data = json.path("data").get(0);
            assertThat(data.path("item").get(0).asText()).isEqualTo("005930");
            assertThat(data.path("type").get(0).asText()).isEqualTo("00");
        }
    }

    @Nested
    @DisplayName("실시간 타입 상수 테스트")
    class RealTypeConstantTest {

        @Test
        @DisplayName("실시간 타입 상수가 올바르게 정의됨")
        void realTypeConstants_defined() {
            assertThat(KiwoomWebSocketClient.RealType.TICK_DATA).isEqualTo("00");
            assertThat(KiwoomWebSocketClient.RealType.STOCK_QUOTE).isEqualTo("0A");
            assertThat(KiwoomWebSocketClient.RealType.PROGRAM_TRADE).isEqualTo("0w");
            assertThat(KiwoomWebSocketClient.RealType.VI_EVENT).isEqualTo("1h");
        }
    }
}
