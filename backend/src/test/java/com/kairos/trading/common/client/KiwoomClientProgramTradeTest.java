package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * KiwoomClient 프로그램매매 조회 TDD 테스트.
 * ka90003: 순매수 상위 50
 * ka90004: 종목별 프로그램매매 현황
 */
@DisplayName("KiwoomClient 프로그램매매 조회 테스트")
class KiwoomClientProgramTradeTest {

    private static WireMockServer wireMockServer;
    private KiwoomClient client;

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
        client = new KiwoomClient(
                gatekeeper,
                baseUrl,
                "test-app-key",
                "test-app-secret");
    }

    // ========== ka90003: 순매수 상위 50 ==========

    @Test
    @DisplayName("순매수 상위 50 조회 성공")
    void getProgramTradeTop_shouldReturnTop50() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "prm_netprps_upper_50": [
                                        {
                                            "stk_cd": "005930",
                                            "stk_nm": "삼성전자",
                                            "cur_prc": 72000,
                                            "flu_sig": "2",
                                            "pred_pre": 1000,
                                            "flu_rt": 1.41,
                                            "acc_trde_qty": 5000000,
                                            "prm_sell_amt": 5000000000,
                                            "prm_buy_amt": 15000000000,
                                            "prm_netprps_amt": 10000000000
                                        },
                                        {
                                            "stk_cd": "000660",
                                            "stk_nm": "SK하이닉스",
                                            "cur_prc": 150000,
                                            "flu_sig": "5",
                                            "pred_pre": -2000,
                                            "flu_rt": -1.32,
                                            "acc_trde_qty": 1000000,
                                            "prm_sell_amt": 8000000000,
                                            "prm_buy_amt": 3000000000,
                                            "prm_netprps_amt": -5000000000
                                        }
                                    ]
                                }
                                """)));

        // when
        var result = client.getProgramTradeTop("test-token", "P00101");

        // then
        assertThat(result).isNotNull();
        assertThat(result.stocks()).hasSize(2);

        // 첫 번째 종목: 삼성전자 (순매수)
        var samsung = result.stocks().get(0);
        assertThat(samsung.stockCode()).isEqualTo("005930");
        assertThat(samsung.isNetBuying()).isTrue();
        assertThat(samsung.isStrongBuying()).isTrue(); // 10억 이상
        assertThat(samsung.programNetBuyAmount()).isEqualTo(10_000_000_000L);

        // 두 번째 종목: SK하이닉스 (순매도)
        var hynix = result.stocks().get(1);
        assertThat(hynix.stockCode()).isEqualTo("000660");
        assertThat(hynix.isNetBuying()).isFalse();
        assertThat(hynix.isStrongSelling()).isTrue(); // -50억 <= -10억

        // 헬퍼 메서드 검증
        assertThat(result.getNetBuyingStocks()).hasSize(1);
        assertThat(result.getStrongBuyingStocks()).hasSize(1);
        assertThat(result.totalNetBuyAmount()).isEqualTo(5_000_000_000L); // 100억 - 50억
    }

    @Test
    @DisplayName("순매수 상위 조회 - 빈 결과")
    void getProgramTradeTop_shouldHandleEmptyResult() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "prm_netprps_upper_50": []
                                }
                                """)));

        // when
        var result = client.getProgramTradeTop("test-token", "P10102");

        // then
        assertThat(result.stocks()).isEmpty();
        assertThat(result.totalNetBuyAmount()).isZero();
    }

    // ========== ka90004: 종목별 프로그램매매 ==========

    @Test
    @DisplayName("종목별 프로그램매매 조회 성공")
    void getProgramTradeByStock_shouldReturnStockData() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/api/dostk/stkinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "stk_prm_trde_prst": [
                                        {
                                            "stk_cd": "005930",
                                            "stk_nm": "삼성전자",
                                            "cur_prc": 72000,
                                            "flu_sig": "2",
                                            "pred_pre": 500,
                                            "flu_rt": 0.70,
                                            "acc_trde_qty": 1200000,
                                            "prm_sell_amt": 2000000000,
                                            "prm_buy_amt": 7000000000,
                                            "prm_netprps_amt": 5000000000
                                        }
                                    ]
                                }
                                """)));

        // when
        var result = client.getProgramTradeByStock("test-token", "005930", "20260131", "P00101");

        // then
        assertThat(result).isNotNull();
        assertThat(result.stocks()).hasSize(1);

        var stock = result.stocks().get(0);
        assertThat(stock.stockCode()).isEqualTo("005930");
        assertThat(stock.programNetBuyAmount()).isEqualTo(5_000_000_000L);
        assertThat(stock.isNetBuying()).isTrue();
    }
}
