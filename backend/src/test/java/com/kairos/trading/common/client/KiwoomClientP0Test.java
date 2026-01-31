package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.domain.flow.dto.ForeignTradeResponse;
import com.kairos.trading.domain.technical.dto.QuoteResponse;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * P0 KiwoomClient 테스트 - ka10004(호가), ka10008(외국인수급).
 */
@WireMockTest
class KiwoomClientP0Test {

    private KiwoomClient kiwoomClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMock) {
        String baseUrl = "http://localhost:" + wireMock.getHttpPort();
        ApiGatekeeper gatekeeper = new ApiGatekeeper();
        gatekeeper.initBuckets();

        kiwoomClient = new KiwoomClient(gatekeeper, baseUrl, "test-key", "test-secret");
    }

    @Test
    @DisplayName("[ka10004] 호가 조회")
    void getQuote_returnsAskBidQuotes() {
        // given
        String responseJson = """
                {
                    "stk_nm": "삼성전자",
                    "cur_prc": 72000,
                    "pred_pre": 500,
                    "flu_rt": 0.7,
                    "acc_trde_qty": 10000000,
                    "sell_hoga1": 72100, "sell_hoga_qty1": 1000, "sell_hoga_qty_chg1": 50,
                    "sell_hoga2": 72200, "sell_hoga_qty2": 2000, "sell_hoga_qty_chg2": 100,
                    "sell_hoga3": 72300, "sell_hoga_qty3": 1500, "sell_hoga_qty_chg3": -50,
                    "sell_hoga4": 72400, "sell_hoga_qty4": 3000, "sell_hoga_qty_chg4": 200,
                    "sell_hoga5": 72500, "sell_hoga_qty5": 2500, "sell_hoga_qty_chg5": 0,
                    "buy_hoga1": 71900, "buy_hoga_qty1": 1200, "buy_hoga_qty_chg1": 30,
                    "buy_hoga2": 71800, "buy_hoga_qty2": 1800, "buy_hoga_qty_chg2": -20,
                    "buy_hoga3": 71700, "buy_hoga_qty3": 2200, "buy_hoga_qty_chg3": 100,
                    "buy_hoga4": 71600, "buy_hoga_qty4": 1600, "buy_hoga_qty_chg4": 50,
                    "buy_hoga5": 71500, "buy_hoga_qty5": 2000, "buy_hoga_qty_chg5": -10
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/quote"))
                .willReturn(okJson(responseJson)));

        // when
        QuoteResponse result = kiwoomClient.getQuote("test-token", "005930");

        // then
        assertThat(result.stockCode()).isEqualTo("005930");
        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.currentPrice()).isEqualTo(72000);
        assertThat(result.askQuotes()).hasSize(5);
        assertThat(result.bidQuotes()).hasSize(5);
        assertThat(result.askQuotes().get(0).price()).isEqualTo(72100);
        assertThat(result.bidQuotes().get(0).price()).isEqualTo(71900);
        assertThat(result.getSpread()).isGreaterThan(0);
        assertThat(result.getBidAskRatio()).isGreaterThan(0);
    }

    @Test
    @DisplayName("[ka10008] 외국인 매매동향 조회")
    void getForeignTrade_returnsDailyData() {
        // given
        String responseJson = """
                {
                    "stk_nm": "삼성전자",
                    "stk_frgn_trde_stts": [
                        {"trde_dt": "20260131", "cls_prc": 72000, "pred_pre": 500, "flu_rt": 0.7, "acc_trde_qty": 5000000, "frgn_netprps_qty": 100000, "frgn_hold_qty": 2000000, "frgn_rt": 55.5},
                        {"trde_dt": "20260130", "cls_prc": 71500, "pred_pre": 300, "flu_rt": 0.42, "acc_trde_qty": 4500000, "frgn_netprps_qty": 50000, "frgn_hold_qty": 1900000, "frgn_rt": 55.3},
                        {"trde_dt": "20260129", "cls_prc": 71200, "pred_pre": -200, "flu_rt": -0.28, "acc_trde_qty": 3800000, "frgn_netprps_qty": -30000, "frgn_hold_qty": 1850000, "frgn_rt": 55.1}
                    ]
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/frgn"))
                .willReturn(okJson(responseJson)));

        // when
        ForeignTradeResponse result = kiwoomClient.getForeignTrade("test-token", "005930");

        // then
        assertThat(result.stockCode()).isEqualTo("005930");
        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.dailyData()).hasSize(3);
        assertThat(result.dailyData().get(0).foreignNetBuy()).isEqualTo(100000);
        assertThat(result.getRecentNetBuy(3)).isEqualTo(120000); // 100000 + 50000 - 30000
        assertThat(result.getConsecutiveBuyDays()).isEqualTo(2); // 처음 2일 순매수
    }
}
