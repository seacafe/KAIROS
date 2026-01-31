package com.kairos.trading.infrastructure.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.kairos.trading.common.client.KiwoomClient;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.gateway.ApiType;
import com.kairos.trading.common.response.ErrorCode;
import com.kairos.trading.domain.fundamental.dto.StockInfoResponse;
import com.kairos.trading.domain.news.dto.OAuthTokenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("KiwoomClient Resilience Test (WireMock)")
class KiwoomClientTest {

    private WireMockServer wireMockServer;
    private KiwoomClient kiwoomClient;

    @Mock
    private ApiGatekeeper gatekeeper;

    @BeforeEach
    void setUp() {
        // Start WireMock Server on a random port
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Configure ApiGatekeeper to execute logic immediately (bypass rate limiting
        // for test)
        given(gatekeeper.execute(eq(ApiType.KIWOOM), any())).willAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        // Initialize KiwoomClient with WireMock base URL
        kiwoomClient = new KiwoomClient(gatekeeper, wireMockServer.baseUrl(), "test-app-key", "test-app-secret");
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("토큰 발급 성공: 200 OK 응답을 정상적으로 파싱해야 한다.")
    void issueToken_Success() {
        // given
        stubFor(post(urlEqualTo("/oauth2/token"))
                .withRequestBody(containing("grant_type"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"access_token\": \"mock-token\", \"expires_in\": 3600, \"token_type\": \"Bearer\"}")));

        // when
        OAuthTokenResponse response = kiwoomClient.issueToken();

        // then
        assertThat(response.accessToken()).isEqualTo("mock-token");
        verify(postRequestedFor(urlEqualTo("/oauth2/token")));
    }

    @Test
    @DisplayName("토큰 발급 실패: 500 에러 발생 시 BusinessException을 던져야 한다.")
    void issueToken_Failure_500() {
        // given
        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // when & then
        assertThatThrownBy(() -> kiwoomClient.issueToken())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KIWOOM_API_ERROR);
    }

    @Test
    @DisplayName("주식정보 조회 성공: 200 OK 및 JSON 파싱 검증")
    void getStockInfo_Success() {
        // given
        stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .withQueryParam("stk_cd", equalTo("005930"))
                .withHeader("Authorization", equalTo("Bearer mock-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "stk_cd": "005930",
                                    "stk_nm": "Samsung Electronics",
                                    "cur_prc": "70000",
                                    "prev_cls": "69000",
                                    "opn_prc": "69500",
                                    "high_prc": "70500",
                                    "low_prc": "69500",
                                    "trd_vol": "1000000",
                                    "chg_rate": "1.45",
                                    "per": "10.5",
                                    "pbr": "1.2",
                                    "eps": "6500",
                                    "mkt_tp": "KOSPI"
                                }
                                """)));

        // when
        StockInfoResponse response = kiwoomClient.getStockInfo("005930", "mock-token");

        // then
        assertThat(response.stockCode()).isEqualTo("005930");
        assertThat(response.currentPrice()).isEqualTo(70000L);
    }

    @Test
    @DisplayName("Rate Limit 초과: 429 에러 발생 시 KIWOOM_RATE_LIMIT_EXCEEDED 예외 발생")
    void getStockInfo_RateLimit_429() {
        // given
        stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withBody("Too Many Requests")));

        // when & then
        assertThatThrownBy(() -> kiwoomClient.getStockInfo("005930", "mock-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("응답 지연: 타임아웃(5s) 발생 시 BusinessException으로 감싸서 던져야 한다.")
    void getStockInfo_Timeout() {
        // given
        stubFor(get(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5500))); // 5.5초 지연 (Client Timeout 5초)

        // when & then
        assertThatThrownBy(() -> kiwoomClient.getStockInfo("005930", "mock-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KIWOOM_API_ERROR);
        // Note: RestClient Timeout is typically wrapped in ResourceAccessException ->
        // caught as Exception -> KIWOOM_API_ERROR
    }
}
