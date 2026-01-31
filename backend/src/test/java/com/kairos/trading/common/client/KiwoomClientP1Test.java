package com.kairos.trading.common.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.domain.technical.dto.ChangeRateTop;
import com.kairos.trading.domain.technical.dto.ViStocksResponse;
import com.kairos.trading.domain.technical.dto.VolumeSpike;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * P1 KiwoomClient 테스트 - ka10023(거래량급증), ka10027(등락률), ka10054(VI).
 */
@WireMockTest
class KiwoomClientP1Test {

    private KiwoomClient kiwoomClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMock) {
        String baseUrl = "http://localhost:" + wireMock.getHttpPort();
        ApiGatekeeper gatekeeper = new ApiGatekeeper();
        gatekeeper.initBuckets();

        kiwoomClient = new KiwoomClient(gatekeeper, baseUrl, "test-key", "test-secret");
    }

    @Test
    @DisplayName("[ka10023] 거래량 급증 조회")
    void getVolumeSpike_returnsStocks() {
        // given
        String responseJson = """
                {
                    "vol_spike_list": [
                        {"stk_cd": "005930", "stk_nm": "삼성전자", "cur_prc": 72000, "flu_sig": "2", "pred_pre": 1000, "flu_rt": 1.4, "acc_trde_qty": 15000000, "vol_rt": 250.5, "trde_amt": 1080000000000},
                        {"stk_cd": "000660", "stk_nm": "SK하이닉스", "cur_prc": 135000, "flu_sig": "2", "pred_pre": 2000, "flu_rt": 1.5, "acc_trde_qty": 8000000, "vol_rt": 180.2, "trde_amt": 1080000000000}
                    ]
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/volspike"))
                .willReturn(okJson(responseJson)));

        // when
        VolumeSpike result = kiwoomClient.getVolumeSpike("test-token", "0");

        // then
        assertThat(result.stocks()).hasSize(2);
        assertThat(result.stocks().get(0).stockCode()).isEqualTo("005930");
        assertThat(result.stocks().get(0).volumeRate()).isEqualTo(250.5);
    }

    @Test
    @DisplayName("[ka10027] 등락률 상위 조회")
    void getChangeRateTop_returnsStocks() {
        // given
        String responseJson = """
                {
                    "flu_rate_list": [
                        {"stk_cd": "005930", "stk_nm": "삼성전자", "cur_prc": 72000, "flu_sig": "2", "pred_pre": 5000, "flu_rt": 7.5, "acc_trde_qty": 20000000, "prev_cls_prc": 67000},
                        {"stk_cd": "000660", "stk_nm": "SK하이닉스", "cur_prc": 140000, "flu_sig": "2", "pred_pre": 8000, "flu_rt": 6.1, "acc_trde_qty": 10000000, "prev_cls_prc": 132000}
                    ]
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/flurate"))
                .willReturn(okJson(responseJson)));

        // when
        ChangeRateTop result = kiwoomClient.getChangeRateTop("test-token", "0", "1");

        // then
        assertThat(result.stocks()).hasSize(2);
        assertThat(result.stocks().get(0).changeRate()).isEqualTo(7.5);
        assertThat(result.stocks().get(1).prevClose()).isEqualTo(132000);
    }

    @Test
    @DisplayName("[ka10054] VI 발동 종목 조회")
    void getViStocks_returnsViList() {
        // given
        String responseJson = """
                {
                    "vi_list": [
                        {"stk_cd": "005930", "stk_nm": "삼성전자", "vi_tp": "1", "vi_stts": "발동", "trig_prc": 75000, "ref_prc": 72000, "trig_tm": "093015"},
                        {"stk_cd": "000660", "stk_nm": "SK하이닉스", "vi_tp": "2", "vi_stts": "해제", "trig_prc": 145000, "ref_prc": 140000, "trig_tm": "101530"}
                    ]
                }
                """;
        stubFor(get(urlPathEqualTo("/api/dostk/vistocks"))
                .willReturn(okJson(responseJson)));

        // when
        ViStocksResponse result = kiwoomClient.getViStocks("test-token");

        // then
        assertThat(result.stocks()).hasSize(2);
        assertThat(result.stocks().get(0).viStatus()).isEqualTo("발동");
        assertThat(result.stocks().get(1).viType()).isEqualTo("2"); // 동적 VI
    }
}
