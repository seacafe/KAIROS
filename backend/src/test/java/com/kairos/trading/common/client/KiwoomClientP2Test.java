package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.domain.flow.dto.ThemeGroupResponse;
import com.kairos.trading.domain.flow.dto.ThemeStocksResponse;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * P2 KiwoomClient 테스트 - ka90001(테마그룹), ka90002(테마종목).
 */
@WireMockTest
class KiwoomClientP2Test {

    private KiwoomClient kiwoomClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMock) {
        String baseUrl = "http://localhost:" + wireMock.getHttpPort();
        ApiGatekeeper gatekeeper = new ApiGatekeeper();
        gatekeeper.initBuckets();

        kiwoomClient = new KiwoomClient(gatekeeper, baseUrl, "test-key", "test-secret");
    }

    @Test
    @DisplayName("[ka90001] 테마 그룹 조회")
    void getThemeGroups_returnsThemes() {
        // given
        String responseJson = """
                {
                    "theme_list": [
                        {"thm_cd": "THM001", "thm_nm": "2차전지", "stk_cnt": 45, "avg_flu_rt": 2.5, "avg_vol": 5000000},
                        {"thm_cd": "THM002", "thm_nm": "반도체", "stk_cnt": 38, "avg_flu_rt": 1.8, "avg_vol": 8000000}
                    ]
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/theme"))
                .willReturn(okJson(responseJson)));

        // when
        ThemeGroupResponse result = kiwoomClient.getThemeGroups("test-token");

        // then
        assertThat(result.themes()).hasSize(2);
        assertThat(result.themes().get(0).themeCode()).isEqualTo("THM001");
        assertThat(result.themes().get(0).themeName()).isEqualTo("2차전지");
        assertThat(result.themes().get(0).stockCount()).isEqualTo(45);
    }

    @Test
    @DisplayName("[ka90002] 테마 구성 종목 조회")
    void getThemeStocks_returnsStocks() {
        // given
        String responseJson = """
                {
                    "thm_nm": "2차전지",
                    "thm_stk_list": [
                        {"stk_cd": "051910", "stk_nm": "LG화학", "cur_prc": 480000, "pred_pre": 15000, "flu_rt": 3.2, "acc_trde_qty": 500000},
                        {"stk_cd": "006400", "stk_nm": "삼성SDI", "cur_prc": 520000, "pred_pre": 10000, "flu_rt": 2.0, "acc_trde_qty": 350000}
                    ]
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/themestk"))
                .willReturn(okJson(responseJson)));

        // when
        ThemeStocksResponse result = kiwoomClient.getThemeStocks("test-token", "THM001");

        // then
        assertThat(result.themeCode()).isEqualTo("THM001");
        assertThat(result.themeName()).isEqualTo("2차전지");
        assertThat(result.stocks()).hasSize(2);
        assertThat(result.stocks().get(0).stockName()).isEqualTo("LG화학");
    }
}
