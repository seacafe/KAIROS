package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.response.ErrorCode;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KiwoomClient 통합 테스트.
 * WireMock을 사용하여 키움 API 응답을 모킹한다.
 */
@DisplayName("KiwoomClient 테스트")
class KiwoomClientTest {

    private static WireMockServer wireMockServer;
    private KiwoomClient kiwoomClient;

    @BeforeAll
    static void setUpServer() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void tearDownServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        var gatekeeper = new ApiGatekeeper();
        gatekeeper.initBuckets();

        var baseUrl = "http://localhost:" + wireMockServer.port();
        kiwoomClient = new KiwoomClient(gatekeeper, baseUrl, "test-api-id", "test-secret");
    }

    @Test
    @DisplayName("토큰 발급 성공 (au10001)")
    void issueToken_shouldReturnToken_whenResponseIsValid() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                    "token_type": "Bearer",
                                    "expires_in": 86400
                                }
                                """)));

        // when
        var result = kiwoomClient.issueToken();

        // then
        assertThat(result).isNotNull();
        assertThat(result.token()).startsWith("eyJ");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(86400);
    }

    @Test
    @DisplayName("토큰 발급 실패 시 예외 발생")
    void issueToken_shouldThrowException_whenResponseIsError() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "error": "invalid_client",
                                    "error_description": "Client authentication failed"
                                }
                                """)));

        // when & then
        assertThatThrownBy(() -> kiwoomClient.issueToken())
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.KIWOOM_API_ERROR);
                });
    }

    @Test
    @DisplayName("주식 기본정보 조회 성공 (ka10001)")
    void getStockInfo_shouldReturnStockInfo_whenResponseIsValid() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .withQueryParam("stk_cd", equalTo("005930"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "stk_cd": "005930",
                                    "stk_nm": "삼성전자",
                                    "cur_prc": 72000,
                                    "prev_cls": 71500,
                                    "opn_prc": 71800,
                                    "high_prc": 72500,
                                    "low_prc": 71600,
                                    "trd_vol": 15000000,
                                    "chg_rate": 0.70,
                                    "per": 12.5,
                                    "pbr": 1.2,
                                    "eps": 5760,
                                    "mkt_tp": "KOSPI"
                                }
                                """)));

        // when
        var result = kiwoomClient.getStockInfo("005930", "test-token");

        // then
        assertThat(result).isNotNull();
        assertThat(result.stockCode()).isEqualTo("005930");
        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.currentPrice()).isEqualTo(72000);
        assertThat(result.per()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("연속조회 헤더가 올바르게 처리된다 (cont-yn)")
    void getStockInfo_shouldHandleContinuationHeader() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .withHeader("cont-yn", equalTo("N"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("cont-yn", "Y")
                        .withHeader("next-key", "ABC123")
                        .withBody("""
                                {
                                    "stk_cd": "005930",
                                    "stk_nm": "삼성전자",
                                    "cur_prc": 72000,
                                    "prev_cls": 71500,
                                    "opn_prc": 71800,
                                    "high_prc": 72500,
                                    "low_prc": 71600,
                                    "trd_vol": 15000000,
                                    "chg_rate": 0.70,
                                    "per": 12.5,
                                    "pbr": 1.2,
                                    "eps": 5760,
                                    "mkt_tp": "KOSPI"
                                }
                                """)));

        // when
        var result = kiwoomClient.getStockInfo("005930", "test-token");

        // then: 첫 번째 요청에서 cont-yn=N 헤더가 전송되어야 함
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/dostk/stkinfo"))
                .withHeader("cont-yn", equalTo("N")));
    }

    @Test
    @DisplayName("API 타임아웃 시 예외 발생")
    void getStockInfo_shouldThrowException_whenTimeout() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000) // 6초 지연
                        .withBody("{}")));

        // when & then
        assertThatThrownBy(() -> kiwoomClient.getStockInfo("005930", "test-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.KIWOOM_API_ERROR);
                });
    }

    @Test
    @DisplayName("Rate Limit 초과 응답(429) 시 예외 발생")
    void getStockInfo_shouldThrowException_whenRateLimitExceeded() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withBody("""
                                {
                                    "error": "rate_limit_exceeded",
                                    "error_description": "Too many requests"
                                }
                                """)));

        // when & then
        assertThatThrownBy(() -> kiwoomClient.getStockInfo("005930", "test-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                });
    }
}
