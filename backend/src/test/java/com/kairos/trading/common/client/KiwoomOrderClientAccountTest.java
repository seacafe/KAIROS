package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * KiwoomOrderClient 계좌/잔고 조회 TDD 테스트.
 * 
 * Red Step: 테스트 먼저 작성 → 실패 확인
 * Green Step: 최소 구현 → 통과
 */
@DisplayName("KiwoomOrderClient 계좌/잔고 조회 테스트")
class KiwoomOrderClientAccountTest {

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

    // ========== 계좌 평가 현황 (kt00004) ==========

    @Test
    @DisplayName("계좌 평가 현황 조회 성공 - 보유종목 포함")
    void getAccountEvaluation_shouldReturnWithHoldings() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/acntinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "acnt_nm": "테스트계좌",
                                    "entr": 5000000,
                                    "d2_entra": 4500000,
                                    "tot_est_amt": 10000000,
                                    "tot_pur_amt": 9000000,
                                    "tdy_lspft": 500000,
                                    "tdy_lspft_rt": 5.5,
                                    "lspft": 1000000,
                                    "lspft_rt": 11.1,
                                    "stk_acnt_evlt_prst": [
                                        {
                                            "stk_cd": "005930",
                                            "stk_nm": "삼성전자",
                                            "rmnd_qty": 10,
                                            "avg_prc": 70000,
                                            "cur_prc": 72000,
                                            "evlt_amt": 720000,
                                            "pl_amt": 20000,
                                            "pl_rt": 2.86,
                                            "pur_amt": 700000,
                                            "tdy_buyq": 0,
                                            "tdy_sellq": 0
                                        },
                                        {
                                            "stk_cd": "000660",
                                            "stk_nm": "SK하이닉스",
                                            "rmnd_qty": 5,
                                            "avg_prc": 150000,
                                            "cur_prc": 140000,
                                            "evlt_amt": 700000,
                                            "pl_amt": -50000,
                                            "pl_rt": -6.67,
                                            "pur_amt": 750000,
                                            "tdy_buyq": 5,
                                            "tdy_sellq": 0
                                        }
                                    ]
                                }
                                """)));

        // when
        var result = orderClient.getAccountEvaluation("test-token");

        // then
        assertThat(result).isNotNull();
        assertThat(result.accountName()).isEqualTo("테스트계좌");
        assertThat(result.deposit()).isEqualTo(5000000);
        assertThat(result.d2EstimatedDeposit()).isEqualTo(4500000);
        assertThat(result.totalHoldingCount()).isEqualTo(2);

        // 보유종목 검증
        var holdings = result.holdingStocks();
        assertThat(holdings).hasSize(2);

        var samsung = holdings.get(0);
        assertThat(samsung.stockCode()).isEqualTo("005930");
        assertThat(samsung.isProfitable()).isTrue();
        assertThat(samsung.needsStopLoss()).isFalse();

        var hynix = holdings.get(1);
        assertThat(hynix.stockCode()).isEqualTo("000660");
        assertThat(hynix.isProfitable()).isFalse();
        assertThat(hynix.needsStopLoss()).isTrue(); // -6.67% < -5%

        // 손절 대상 검증
        assertThat(result.stopLossTargets()).hasSize(1);
        assertThat(result.stopLossTargets().get(0).stockCode()).isEqualTo("000660");
    }

    @Test
    @DisplayName("계좌 평가 현황 조회 - 보유종목 없음")
    void getAccountEvaluation_shouldReturnEmptyHoldings() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/acntinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "acnt_nm": "빈계좌",
                                    "entr": 10000000,
                                    "d2_entra": 10000000,
                                    "tot_est_amt": 0,
                                    "tot_pur_amt": 0,
                                    "tdy_lspft": 0,
                                    "tdy_lspft_rt": 0.0,
                                    "lspft": 0,
                                    "lspft_rt": 0.0,
                                    "stk_acnt_evlt_prst": []
                                }
                                """)));

        // when
        var result = orderClient.getAccountEvaluation("test-token");

        // then
        assertThat(result.totalHoldingCount()).isZero();
        assertThat(result.deposit()).isEqualTo(10000000);
    }

    // ========== 체결 잔고 (kt00005) ==========

    @Test
    @DisplayName("체결 잔고 조회 성공")
    void getExecutionBalance_shouldReturnBalance() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/acntinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "entr": 5000000,
                                    "entr_d1": 4800000,
                                    "entr_d2": 4500000,
                                    "pymn_alow_amt": 4000000,
                                    "ord_alowa": 4500000,
                                    "stk_buy_tot_amt": 5000000,
                                    "evlt_amt_tot": 5500000,
                                    "tot_pl_tot": 500000,
                                    "tot_pl_rt": 10.0,
                                    "stk_cntr_remn": [
                                        {
                                            "stk_cd": "005930",
                                            "stk_nm": "삼성전자",
                                            "setl_remn": 10,
                                            "cur_qty": 10,
                                            "cur_prc": 72000,
                                            "buy_uv": 70000,
                                            "pur_amt": 700000,
                                            "evlt_amt": 720000,
                                            "evltv_prft": 20000,
                                            "pl_rt": 2.86,
                                            "crd_tp": "현금",
                                            "loan_dt": "",
                                            "expr_dt": ""
                                        }
                                    ]
                                }
                                """)));

        // when
        var result = orderClient.getExecutionBalance("test-token");

        // then
        assertThat(result).isNotNull();
        assertThat(result.deposit()).isEqualTo(5000000);
        assertThat(result.orderableAmount()).isEqualTo(4500000);
        assertThat(result.canBuy(3000000)).isTrue();
        assertThat(result.canBuy(5000000)).isFalse();
        assertThat(result.executionCount()).isEqualTo(1);

        var stock = result.executionStocks().get(0);
        assertThat(stock.isCashPurchase()).isTrue();
    }
}
