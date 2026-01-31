package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * KiwoomClient 토큰 폐기 및 KiwoomOrderClient 미체결/체결 조회 테스트.
 */
class KiwoomClientRevokeAndOrdersTest {

    private static WireMockServer wireMock;
    private KiwoomClient kiwoomClient;
    private KiwoomOrderClient kiwoomOrderClient;

    @BeforeAll
    static void setupWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void tearDown() {
        wireMock.stop();
    }

    @BeforeEach
    void setup() {
        String baseUrl = "http://localhost:" + wireMock.port();
        ApiGatekeeper gatekeeper = new ApiGatekeeper();
        gatekeeper.initBuckets(); // @PostConstruct 수동 호출

        kiwoomClient = new KiwoomClient(gatekeeper, baseUrl, "test-key", "test-secret");
        kiwoomOrderClient = new KiwoomOrderClient(gatekeeper, baseUrl, "test-key", "test-secret", "12345678-01", true);

        wireMock.resetAll();
    }

    // ===== revokeToken 테스트 =====

    @Test
    @DisplayName("[au10002] 토큰 폐기 - 성공")
    void revokeToken_success() {
        // given
        wireMock.stubFor(post(urlEqualTo("/oauth2/revoke"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"result\":\"success\"}")));

        // when
        boolean result = kiwoomClient.revokeToken("test-token");

        // then
        assertThat(result).isTrue();
    }

    // ===== getUnfilledOrders 테스트 =====

    @Test
    @DisplayName("[kt10002] 미체결 조회 - 성공")
    void getUnfilledOrders_success() {
        // given
        String responseJson = """
                {
                    "output": [
                        {
                            "orgn_ord_no": "0001234567",
                            "stk_cd": "005930",
                            "stk_nm": "삼성전자",
                            "buy_sl_tp": "01",
                            "ord_qty": 10,
                            "ord_pric": 85000,
                            "not_ccld_qty": 5,
                            "ccld_qty": 5,
                            "ord_dt": "20260131",
                            "ord_tm": "093000"
                        }
                    ]
                }
                """;
        wireMock.stubFor(post(urlEqualTo("/api/dostk/order"))
                .withHeader("tr_id", equalTo("kt10002"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        // when
        var orders = kiwoomOrderClient.getUnfilledOrders("test-token");

        // then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).stockCode()).isEqualTo("005930");
        assertThat(orders.get(0).orderType()).isEqualTo("BUY");
        assertThat(orders.get(0).unfilledQty()).isEqualTo(5);
    }

    // ===== getExecutedOrders 테스트 =====

    @Test
    @DisplayName("[kt10003] 당일 체결 내역 조회 - 성공")
    void getExecutedOrders_success() {
        // given
        String responseJson = """
                {
                    "output": [
                        {
                            "ord_no": "0001234567",
                            "stk_cd": "005930",
                            "stk_nm": "삼성전자",
                            "buy_sl_tp": "01",
                            "ccld_qty": 100,
                            "ccld_pric": 85000,
                            "ccld_amt": 8500000,
                            "ccld_dt": "20260131",
                            "ccld_tm": "091530"
                        },
                        {
                            "ord_no": "0001234568",
                            "stk_cd": "000660",
                            "stk_nm": "SK하이닉스",
                            "buy_sl_tp": "02",
                            "ccld_qty": 50,
                            "ccld_pric": 180000,
                            "ccld_amt": 9000000,
                            "ccld_dt": "20260131",
                            "ccld_tm": "140030"
                        }
                    ]
                }
                """;
        wireMock.stubFor(post(urlEqualTo("/api/dostk/order"))
                .withHeader("tr_id", equalTo("kt10003"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        // when
        var orders = kiwoomOrderClient.getExecutedOrders("test-token");

        // then
        assertThat(orders).hasSize(2);

        // 첫 번째 체결 (매수)
        assertThat(orders.get(0).stockCode()).isEqualTo("005930");
        assertThat(orders.get(0).orderType()).isEqualTo("BUY");
        assertThat(orders.get(0).filledQty()).isEqualTo(100);
        assertThat(orders.get(0).filledAmount()).isEqualTo(8500000L);

        // 두 번째 체결 (매도)
        assertThat(orders.get(1).stockCode()).isEqualTo("000660");
        assertThat(orders.get(1).orderType()).isEqualTo("SELL");
    }

    @Test
    @DisplayName("[kt10003] 체결 없음 - 빈 리스트 반환")
    void getExecutedOrders_empty() {
        // given
        wireMock.stubFor(post(urlEqualTo("/api/dostk/order"))
                .withHeader("tr_id", equalTo("kt10003"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"output\":[]}")));

        // when
        var orders = kiwoomOrderClient.getExecutedOrders("test-token");

        // then
        assertThat(orders).isEmpty();
    }
}
