package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.response.ErrorCode;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KiwoomOrderClient TDD 테스트.
 * 
 * Red-Green-Refactor:
 * 1. 이 테스트를 먼저 작성 (Red - 실패)
 * 2. KiwoomOrderClient 구현 (Green - 통과)
 * 3. 리팩토링 (Refactor)
 */
@DisplayName("KiwoomOrderClient 주문 API 테스트")
class KiwoomOrderClientTest {

    private static WireMockServer wireMockServer;
    private KiwoomOrderClient orderClient;

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
        orderClient = new KiwoomOrderClient(
                gatekeeper,
                baseUrl,
                "test-app-key",
                "test-app-secret",
                "00000000-00",
                true // 모의투자
        );
    }

    // ========== 예수금 조회 (kt00004) ==========

    @Test
    @DisplayName("예수금 조회 성공 (kt00004)")
    void getBalance_shouldReturnBalance_whenResponseIsValid() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/dostk/acntinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "ord_psbl_cash": 5000000,
                                    "tot_evlu_amt": 10000000,
                                    "pchs_amt_smtl": 5000000,
                                    "evlu_pfls_smtl": 500000,
                                    "evlu_pfls_rt": 10.0
                                }
                                """)));

        // when
        var result = orderClient.getBalance("test-token");

        // then
        assertThat(result).isNotNull();
        assertThat(result.availableAmount()).isEqualTo(5000000);
        assertThat(result.totalEvalAmount()).isEqualTo(10000000);
        assertThat(result.profitLossRate()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("예수금 조회 실패 시 예외 발생")
    void getBalance_shouldThrowException_whenResponseIsError() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/api/dostk/acntinfo"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("{\"error\": \"Unauthorized\"}")));

        // when & then
        assertThatThrownBy(() -> orderClient.getBalance("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.KIWOOM_API_ERROR);
                });
    }

    // ========== 매수 주문 (kt10000) ==========

    @Test
    @DisplayName("매수 주문 성공 (kt10000)")
    void submitBuyOrder_shouldReturnOrderId_whenSuccess() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "0",
                                    "msg_cd": "APBK0013",
                                    "msg1": "주문 전송 완료",
                                    "output": {
                                        "ord_no": "0000123456",
                                        "ord_tmd": "093015"
                                    }
                                }
                                """)));

        // when
        var result = orderClient.submitBuyOrder("test-token", "005930", 10, 72000);

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo("0000123456");
        assertThat(result.isSuccess()).isTrue();

        // 요청 본문 검증
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/dostk/order"))
                .withRequestBody(containing("\"ord_dvsn\":\"00\"")) // 지정가
                .withRequestBody(containing("\"sll_buy_dvsn_cd\":\"02\"")) // 매수
                .withRequestBody(containing("\"pdno\":\"005930\"")));
    }

    @Test
    @DisplayName("매수 주문 실패 - 예수금 부족")
    void submitBuyOrder_shouldThrowException_whenInsufficientFunds() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "-1",
                                    "msg_cd": "APBK0014",
                                    "msg1": "주문가능금액 부족"
                                }
                                """)));

        // when & then
        assertThatThrownBy(() -> orderClient.submitBuyOrder("test-token", "005930", 1000, 72000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.KIWOOM_ORDER_FAILED);
                });
    }

    // ========== 매도 주문 (kt10001) ==========

    @Test
    @DisplayName("매도 주문 성공 (kt10001)")
    void submitSellOrder_shouldReturnOrderId_whenSuccess() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "0",
                                    "msg_cd": "APBK0013",
                                    "msg1": "주문 전송 완료",
                                    "output": {
                                        "ord_no": "0000123457",
                                        "ord_tmd": "143015"
                                    }
                                }
                                """)));

        // when
        var result = orderClient.submitSellOrder("test-token", "005930", 10, 73000);

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo("0000123457");
        assertThat(result.isSuccess()).isTrue();

        // 매도 코드 검증
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/dostk/order"))
                .withRequestBody(containing("\"sll_buy_dvsn_cd\":\"01\""))); // 매도
    }

    // ========== 시장가 매도 (Kill Switch) ==========

    @Test
    @DisplayName("시장가 매도 성공 (Kill Switch)")
    void submitMarketSellOrder_shouldExecute() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "0",
                                    "msg_cd": "APBK0013",
                                    "msg1": "주문 전송 완료",
                                    "output": {
                                        "ord_no": "0000123458",
                                        "ord_tmd": "093500"
                                    }
                                }
                                """)));

        // when
        var result = orderClient.submitMarketSellOrder("test-token", "005930", 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 시장가 주문 코드 검증
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/dostk/order"))
                .withRequestBody(containing("\"ord_dvsn\":\"01\""))); // 시장가
    }

    // ========== Rate Limit 처리 ==========

    @Test
    @DisplayName("Rate Limit 초과 응답(429) 시 예외 발생")
    void submitOrder_shouldThrowException_whenRateLimitExceeded() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withBody("{\"error\": \"rate_limit_exceeded\"}")));

        // when & then
        assertThatThrownBy(() -> orderClient.submitBuyOrder("test-token", "005930", 10, 72000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                });
    }

    // ========== 주문 정정 (kt10001 - Amend) ==========

    @Test
    @DisplayName("주문 정정 성공")
    void amendOrder_shouldReturnOrderId_whenSuccess() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "0",
                                    "msg_cd": "APBK0013",
                                    "msg1": "정정 완료",
                                    "output": {
                                        "ord_no": "0000123459",
                                        "ord_tmd": "100530"
                                    }
                                }
                                """)));

        // when
        var result = orderClient.amendOrder("test-token", "0000123456", "005930", 5, 73000);

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo("0000123459");
        assertThat(result.isSuccess()).isTrue();

        // 정정 관련 필드 검증
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/dostk/order"))
                .withRequestBody(containing("\"orgn_odno\":\"0000123456\""))
                .withRequestBody(containing("\"rvse_cncl_dvsn_cd\":\"01\""))); // 정정
    }

    // ========== 주문 취소 (kt10003 - Cancel) ==========

    @Test
    @DisplayName("주문 취소 성공 - 부분 취소")
    void cancelOrder_shouldReturnOrderId_whenPartialCancel() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "0",
                                    "msg_cd": "APBK0013",
                                    "msg1": "취소 완료",
                                    "output": {
                                        "ord_no": "0000123460",
                                        "ord_tmd": "101500"
                                    }
                                }
                                """)));

        // when
        var result = orderClient.cancelOrder("test-token", "0000123456", "005930", 3);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 취소 관련 필드 검증
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/dostk/order"))
                .withRequestBody(containing("\"rvse_cncl_dvsn_cd\":\"02\"")) // 취소
                .withRequestBody(containing("\"qty_all_ord_yn\":\"N\""))); // 부분 취소
    }

    @Test
    @DisplayName("주문 취소 성공 - 전량 취소")
    void cancelOrder_shouldReturnOrderId_whenFullCancel() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/order"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "rt_cd": "0",
                                    "msg_cd": "APBK0013",
                                    "msg1": "전량 취소 완료",
                                    "output": {
                                        "ord_no": "0000123461",
                                        "ord_tmd": "102000"
                                    }
                                }
                                """)));

        // when (수량 0 = 전량 취소)
        var result = orderClient.cancelOrder("test-token", "0000123456", "005930", 0);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 전량 취소 필드 검증
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/dostk/order"))
                .withRequestBody(containing("\"qty_all_ord_yn\":\"Y\""))); // 전량 취소
    }
}
