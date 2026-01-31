package com.kairos.trading.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.gateway.ApiType;
import com.kairos.trading.common.response.ErrorCode;
import com.kairos.trading.domain.fundamental.dto.StockInfoResponse;
import com.kairos.trading.domain.news.dto.OAuthTokenResponse;
import com.kairos.trading.domain.technical.dto.DailyChartResponse;
import com.kairos.trading.domain.technical.dto.DailyChartResponse.DailyCandle;
import com.kairos.trading.domain.technical.dto.MinuteChartResponse;
import com.kairos.trading.domain.technical.dto.MinuteChartResponse.MinuteCandle;
import com.kairos.trading.domain.flow.dto.ProgramTradeResponse;
import com.kairos.trading.domain.flow.dto.ProgramTradeResponse.ProgramTradeStock;
import com.kairos.trading.domain.flow.dto.ForeignTradeResponse;
import com.kairos.trading.domain.technical.dto.QuoteResponse;
import com.kairos.trading.domain.technical.dto.VolumeSpike;
import com.kairos.trading.domain.technical.dto.ChangeRateTop;
import com.kairos.trading.domain.technical.dto.ViStocksResponse;
import com.kairos.trading.domain.flow.dto.ThemeGroupResponse;
import com.kairos.trading.domain.flow.dto.ThemeStocksResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 키움증권 Open API 연동 클라이언트.
 *
 * 역할:
 * 1. OAuth2 토큰 발급 및 관리
 * 2. 주식 기본정보 및 호가 조회
 * 3. 주문 전송 (별도 분리 가능)
 */
@Slf4j
@Component
public class KiwoomClient {

    private final ApiGatekeeper gatekeeper;
    private final RestClient restClient;
    private final String appKey;
    private final String appSecret;
    private final ObjectMapper objectMapper;

    public KiwoomClient(ApiGatekeeper gatekeeper,
            @Value("${kiwoom.api.base-url}") String baseUrl,
            @Value("${kiwoom.api.app-key}") String appKey,
            @Value("${kiwoom.api.app-secret}") String appSecret) {
        this.gatekeeper = gatekeeper;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.objectMapper = new ObjectMapper();

        // 5초 타임아웃 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 접근 토큰(Access Token) 발급 - [au10001]
     */
    public OAuthTokenResponse issueToken() {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("키움 API 토큰 발급 요청");
            try {
                var response = restClient.post()
                        .uri("/oauth2/token")
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(String.format(
                                "{\"grant_type\":\"client_credentials\",\"appkey\":\"%s\",\"appsecret\":\"%s\"}",
                                appKey, appSecret))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            log.error("토큰 발급 실패: {} - {}", res.getStatusCode(), res.getStatusText());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "Token issue failed");
                        })
                        .body(OAuthTokenResponse.class);

                return response;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("토큰 발급 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 접근 토큰 폐기 - [au10002]
     * 장 종료 후 또는 토큰 강제 갱신 시 호출.
     * 
     * @param token 폐기할 토큰
     * @return 폐기 성공 여부
     */
    public boolean revokeToken(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("키움 API 토큰 폐기 요청");
            try {
                var response = restClient.post()
                        .uri("/oauth2/revoke")
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(String.format(
                                "{\"appkey\":\"%s\",\"appsecret\":\"%s\",\"token\":\"%s\"}",
                                appKey, appSecret, token))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            log.error("토큰 폐기 실패: {} - {}", res.getStatusCode(), res.getStatusText());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "Token revoke failed");
                        })
                        .body(String.class);

                log.info("토큰 폐기 성공");
                return true;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("토큰 폐기 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 주식 기본정보 조회 - [ka10001]
     */
    public StockInfoResponse getStockInfo(String stockCode, String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/stkinfo")
                                .queryParam("stk_cd", stockCode)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("cont-yn", "N") // 연속조회 여부
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            log.warn("키움 API Rate Limit 초과");
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            log.error("주식정보 조회 실패: {} - {}", res.getStatusCode(), res.getStatusText());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "주식정보 조회 실패");
                        })
                        .body(String.class);

                return parseStockInfoResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("주식정보 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e); // TimeoutException은 여기서 잡혀서
                                                                            // KIWOOM_API_ERROR로 감싸짐
            }
        });
    }

    private StockInfoResponse parseStockInfoResponse(String json) throws Exception {
        var node = objectMapper.readTree(json);
        return new StockInfoResponse(
                node.path("stk_cd").asText(),
                node.path("stk_nm").asText(),
                node.path("cur_prc").asLong(),
                node.path("prev_cls").asLong(),
                node.path("opn_prc").asLong(),
                node.path("high_prc").asLong(),
                node.path("low_prc").asLong(),
                node.path("trd_vol").asLong(),
                node.path("chg_rate").asDouble(),
                node.path("per").asDouble(),
                node.path("pbr").asDouble(),
                node.path("eps").asDouble(),
                node.path("mkt_tp").asText());
    }

    /**
     * 일봉 차트 조회 - [ka10081]
     * 
     * @param stockCode     종목코드
     * @param baseDate      기준일자 (YYYYMMDD)
     * @param adjustedPrice 수정주가 적용 여부
     * @param token         접근 토큰
     * @return 일봉 차트 데이터
     */
    public DailyChartResponse getDailyChart(String stockCode, String baseDate,
            boolean adjustedPrice, String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            try {
                String requestBody = String.format(
                        "{\"stk_cd\":\"%s\",\"base_dt\":\"%s\",\"upd_stkpc_tp\":\"%s\"}",
                        stockCode, baseDate, adjustedPrice ? "1" : "0");

                var response = restClient.post()
                        .uri("/api/dostk/chart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header("tr_id", "ka10081")
                        .body(requestBody)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "일봉 차트 조회 실패");
                        })
                        .body(String.class);

                return parseDailyChartResponse(stockCode, response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("일봉 차트 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 분봉 차트 조회 - [ka10080]
     * 
     * @param stockCode 종목코드
     * @param tickScope 틱범위 (1:1분, 3:3분, 5:5분...)
     * @param token     접근 토큰
     * @return 분봉 차트 데이터
     */
    public MinuteChartResponse getMinuteChart(String stockCode, int tickScope, String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            try {
                String requestBody = String.format(
                        "{\"stk_cd\":\"%s\",\"tic_scope\":\"%d\",\"upd_stkpc_tp\":\"1\"}",
                        stockCode, tickScope);

                var response = restClient.post()
                        .uri("/api/dostk/chart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header("tr_id", "ka10080")
                        .body(requestBody)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "분봉 차트 조회 실패");
                        })
                        .body(String.class);

                return parseMinuteChartResponse(stockCode, response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("분봉 차트 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private DailyChartResponse parseDailyChartResponse(String stockCode, String json) throws Exception {
        var node = objectMapper.readTree(json);
        var dataArray = node.path("stk_dt_pole_chart_qry");

        List<DailyCandle> candles = new ArrayList<>();
        for (JsonNode item : dataArray) {
            candles.add(new DailyCandle(
                    item.path("dt").asText(),
                    item.path("cur_prc").asLong(),
                    item.path("open_pric").asLong(),
                    item.path("high_pric").asLong(),
                    item.path("low_pric").asLong(),
                    item.path("trde_qty").asLong(),
                    item.path("trde_prica").asLong(),
                    item.path("trde_tern_rt").asDouble(),
                    item.path("pred_pre").asLong(),
                    item.path("pred_pre_sig").asText()));
        }

        return new DailyChartResponse(stockCode, candles);
    }

    private MinuteChartResponse parseMinuteChartResponse(String stockCode, String json) throws Exception {
        var node = objectMapper.readTree(json);
        var dataArray = node.path("stk_min_pole_chart_qry");

        List<MinuteCandle> candles = new ArrayList<>();
        for (JsonNode item : dataArray) {
            candles.add(new MinuteCandle(
                    item.path("cntr_tm").asText(),
                    item.path("cur_prc").asLong(),
                    item.path("open_pric").asLong(),
                    item.path("high_pric").asLong(),
                    item.path("low_pric").asLong(),
                    item.path("trde_qty").asLong(),
                    item.path("acc_trde_qty").asLong(),
                    item.path("pred_pre").asLong(),
                    item.path("pred_pre_sig").asText()));
        }

        return new MinuteChartResponse(stockCode, candles);
    }

    // ========== 프로그램매매 조회 (ka90003, ka90004) ==========

    /**
     * 프로그램 순매수 상위 50 조회 (ka90003).
     * Sonar 에이전트가 섹터 흐름 파악에 활용.
     * 
     * @param token      접근 토큰
     * @param marketType 시장구분 (P00101:코스피, P10102:코스닥)
     * @return 프로그램매매 응답
     */
    public ProgramTradeResponse getProgramTradeTop(String token, String marketType) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomClient] 프로그램 순매수 상위 조회: {}", marketType);
            try {
                String requestBody = String.format(
                        "{\"trde_upper_tp\":\"2\",\"amt_qty_tp\":\"1\",\"mrkt_tp\":\"%s\"}",
                        marketType);

                var response = restClient.post()
                        .uri("/api/dostk/stkinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header("tr_id", "ka90003")
                        .body(requestBody)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "프로그램매매 조회 실패");
                        })
                        .body(String.class);

                return parseProgramTradeResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("프로그램매매 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 종목별 프로그램매매 현황 조회 (ka90004).
     * 특정 타겟 종목의 프로그램 유입 확인.
     * 
     * @param token      접근 토큰
     * @param stockCode  종목코드
     * @param date       일자 (YYYYMMDD)
     * @param marketType 시장구분
     * @return 프로그램매매 응답
     */
    public ProgramTradeResponse getProgramTradeByStock(String token, String stockCode, String date, String marketType) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomClient] 종목별 프로그램매매 조회: {} ({})", stockCode, date);
            try {
                String requestBody = String.format(
                        "{\"stk_cd\":\"%s\",\"dt\":\"%s\",\"mrkt_tp\":\"%s\"}",
                        stockCode, date, marketType);

                var response = restClient.post()
                        .uri("/api/dostk/stkinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header("tr_id", "ka90004")
                        .body(requestBody)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "종목별 프로그램매매 조회 실패");
                        })
                        .body(String.class);

                return parseProgramTradeResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("종목별 프로그램매매 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ProgramTradeResponse parseProgramTradeResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<ProgramTradeStock> stocks = new ArrayList<>();

        // ka90003은 prm_netprps_upper_50, ka90004는 stk_prm_trde_prst 사용
        JsonNode dataNode = root.path("prm_netprps_upper_50");
        if (dataNode.isMissingNode() || !dataNode.isArray()) {
            dataNode = root.path("stk_prm_trde_prst");
        }

        if (dataNode.isArray()) {
            int rank = 1;
            for (JsonNode item : dataNode) {
                stocks.add(new ProgramTradeStock(
                        item.has("rank") ? item.path("rank").asInt() : rank++,
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("cur_prc").asInt(),
                        item.path("flu_sig").asText(),
                        item.path("pred_pre").asInt(),
                        item.path("flu_rt").asDouble(),
                        item.path("acc_trde_qty").asLong(),
                        item.path("prm_sell_amt").asLong(),
                        item.path("prm_buy_amt").asLong(),
                        item.path("prm_netprps_amt").asLong()));
            }
        }

        return new ProgramTradeResponse(stocks);
    }

    // =========================================================================
    // P0 추가 API: 호가 및 외국인 수급
    // =========================================================================

    /**
     * 주식 호가 조회 (ka10004).
     * Vector 에이전트가 스프레드 계산 및 진입가 결정에 활용.
     *
     * @param token     접근 토큰
     * @param stockCode 종목코드
     * @return 호가 응답
     */
    public QuoteResponse getQuote(String token, String stockCode) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka10004] 호가 조회: {}", stockCode);
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/quote")
                                .queryParam("stk_cd", stockCode)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka10004")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseQuoteResponse(response, stockCode);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("호가 조회 중 예외 발생: {}", stockCode, e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private QuoteResponse parseQuoteResponse(String json, String stockCode) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        String stockName = root.path("stk_nm").asText();
        int currentPrice = root.path("cur_prc").asInt();
        int priceChange = root.path("pred_pre").asInt();
        double changeRate = root.path("flu_rt").asDouble();
        long accVolume = root.path("acc_trde_qty").asLong();

        // 매도호가 파싱 (1~5)
        List<QuoteResponse.QuoteLevel> askQuotes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            askQuotes.add(new QuoteResponse.QuoteLevel(
                    root.path("sell_hoga" + i).asInt(),
                    root.path("sell_hoga_qty" + i).asLong(),
                    root.path("sell_hoga_qty_chg" + i).asInt()));
        }

        // 매수호가 파싱 (1~5)
        List<QuoteResponse.QuoteLevel> bidQuotes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            bidQuotes.add(new QuoteResponse.QuoteLevel(
                    root.path("buy_hoga" + i).asInt(),
                    root.path("buy_hoga_qty" + i).asLong(),
                    root.path("buy_hoga_qty_chg" + i).asInt()));
        }

        return new QuoteResponse(stockCode, stockName, currentPrice, priceChange,
                changeRate, accVolume, askQuotes, bidQuotes);
    }

    /**
     * 외국인 종목별 매매동향 조회 (ka10008).
     * Sonar 에이전트가 외국인 수급 분석에 활용.
     *
     * @param token     접근 토큰
     * @param stockCode 종목코드
     * @return 외국인 매매동향 응답
     */
    public ForeignTradeResponse getForeignTrade(String token, String stockCode) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka10008] 외국인매매동향 조회: {}", stockCode);
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/frgn")
                                .queryParam("stk_cd", stockCode)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka10008")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseForeignTradeResponse(response, stockCode);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("외국인매매동향 조회 중 예외 발생: {}", stockCode, e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ForeignTradeResponse parseForeignTradeResponse(String json, String stockCode) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        String stockName = root.path("stk_nm").asText();
        List<ForeignTradeResponse.ForeignTradeDay> dailyData = new ArrayList<>();

        JsonNode dataNode = root.path("stk_frgn_trde_stts");
        if (dataNode.isArray()) {
            for (JsonNode item : dataNode) {
                dailyData.add(new ForeignTradeResponse.ForeignTradeDay(
                        item.path("trde_dt").asText(),
                        item.path("cls_prc").asInt(),
                        item.path("pred_pre").asInt(),
                        item.path("flu_rt").asDouble(),
                        item.path("acc_trde_qty").asLong(),
                        item.path("frgn_netprps_qty").asLong(),
                        item.path("frgn_hold_qty").asLong(),
                        item.path("frgn_rt").asDouble()));
            }
        }

        return new ForeignTradeResponse(stockCode, stockName, dailyData);
    }

    // =========================================================================
    // P1 추가 API: 스크리닝/모멘텀
    // =========================================================================

    /**
     * 거래량 급증 종목 조회 (ka10023).
     * Vector 에이전트가 급등 종목 스크리닝에 활용.
     *
     * @param token  접근 토큰
     * @param market "0" 전체, "1" 코스피, "2" 코스닥
     * @return 거래량 급증 종목 리스트
     */
    public VolumeSpike getVolumeSpike(String token, String market) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka10023] 거래량급증 조회: market={}", market);
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/volspike")
                                .queryParam("mkt_tp", market)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka10023")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseVolumeSpikeResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("거래량급증 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private VolumeSpike parseVolumeSpikeResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<VolumeSpike.VolumeSpikeStock> stocks = new ArrayList<>();

        JsonNode dataNode = root.path("vol_spike_list");
        if (dataNode.isArray()) {
            int rank = 1;
            for (JsonNode item : dataNode) {
                stocks.add(new VolumeSpike.VolumeSpikeStock(
                        rank++,
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("cur_prc").asInt(),
                        item.path("flu_sig").asText(),
                        item.path("pred_pre").asInt(),
                        item.path("flu_rt").asDouble(),
                        item.path("acc_trde_qty").asLong(),
                        item.path("vol_rt").asDouble(),
                        item.path("trde_amt").asLong()));
            }
        }

        return new VolumeSpike(stocks);
    }

    /**
     * 등락률 상위 종목 조회 (ka10027).
     * Vector 에이전트가 모멘텀 감지에 활용.
     *
     * @param token  접근 토큰
     * @param market "0" 전체, "1" 코스피, "2" 코스닥
     * @param type   "1" 상승률, "2" 하락률
     * @return 등락률 상위 종목 리스트
     */
    public ChangeRateTop getChangeRateTop(String token, String market, String type) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka10027] 등락률상위 조회: market={}, type={}", market, type);
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/flurate")
                                .queryParam("mkt_tp", market)
                                .queryParam("flu_tp", type)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka10027")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseChangeRateTopResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("등락률상위 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ChangeRateTop parseChangeRateTopResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<ChangeRateTop.ChangeRateStock> stocks = new ArrayList<>();

        JsonNode dataNode = root.path("flu_rate_list");
        if (dataNode.isArray()) {
            int rank = 1;
            for (JsonNode item : dataNode) {
                stocks.add(new ChangeRateTop.ChangeRateStock(
                        rank++,
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("cur_prc").asInt(),
                        item.path("flu_sig").asText(),
                        item.path("pred_pre").asInt(),
                        item.path("flu_rt").asDouble(),
                        item.path("acc_trde_qty").asLong(),
                        item.path("prev_cls_prc").asInt()));
            }
        }

        return new ChangeRateTop(stocks);
    }

    /**
     * VI 발동 종목 조회 (ka10054).
     * Aegis 에이전트가 리스크 관리에 활용.
     *
     * @param token 접근 토큰
     * @return VI 발동 종목 리스트
     */
    public ViStocksResponse getViStocks(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka10054] VI발동종목 조회");
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/vistocks")
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka10054")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseViStocksResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("VI발동종목 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ViStocksResponse parseViStocksResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<ViStocksResponse.ViStock> stocks = new ArrayList<>();

        JsonNode dataNode = root.path("vi_list");
        if (dataNode.isArray()) {
            for (JsonNode item : dataNode) {
                stocks.add(new ViStocksResponse.ViStock(
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("vi_tp").asText(),
                        item.path("vi_stts").asText(),
                        item.path("trig_prc").asInt(),
                        item.path("ref_prc").asInt(),
                        item.path("trig_tm").asText()));
            }
        }

        return new ViStocksResponse(stocks);
    }

    // =========================================================================
    // P2 추가 API: 테마 분석
    // =========================================================================

    /**
     * 테마 그룹별 조회 (ka90001).
     * Nexus 전략가가 테마 분석에 활용.
     *
     * @param token 접근 토큰
     * @return 테마 그룹 리스트
     */
    public ThemeGroupResponse getThemeGroups(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka90001] 테마그룹 조회");
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/theme")
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka90001")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseThemeGroupResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("테마그룹 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ThemeGroupResponse parseThemeGroupResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<ThemeGroupResponse.ThemeGroup> themes = new ArrayList<>();

        JsonNode dataNode = root.path("theme_list");
        if (dataNode.isArray()) {
            for (JsonNode item : dataNode) {
                themes.add(new ThemeGroupResponse.ThemeGroup(
                        item.path("thm_cd").asText(),
                        item.path("thm_nm").asText(),
                        item.path("stk_cnt").asInt(),
                        item.path("avg_flu_rt").asDouble(),
                        item.path("avg_vol").asLong()));
            }
        }

        return new ThemeGroupResponse(themes);
    }

    /**
     * 테마 구성 종목 조회 (ka90002).
     * Nexus 전략가가 테마 연관 종목 분석에 활용.
     *
     * @param token     접근 토큰
     * @param themeCode 테마 코드
     * @return 테마 구성 종목 리스트
     */
    public ThemeStocksResponse getThemeStocks(String token, String themeCode) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.debug("[ka90002] 테마종목 조회: {}", themeCode);
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/themestk")
                                .queryParam("thm_cd", themeCode)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("tr-cd", "ka90002")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .body(String.class);

                return parseThemeStocksResponse(response, themeCode);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("테마종목 조회 중 예외 발생: {}", themeCode, e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ThemeStocksResponse parseThemeStocksResponse(String json, String themeCode) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        String themeName = root.path("thm_nm").asText();
        List<ThemeStocksResponse.ThemeStock> stocks = new ArrayList<>();

        JsonNode dataNode = root.path("thm_stk_list");
        if (dataNode.isArray()) {
            for (JsonNode item : dataNode) {
                stocks.add(new ThemeStocksResponse.ThemeStock(
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("cur_prc").asInt(),
                        item.path("pred_pre").asInt(),
                        item.path("flu_rt").asDouble(),
                        item.path("acc_trde_qty").asLong()));
            }
        }

        return new ThemeStocksResponse(themeCode, themeName, stocks);
    }
}
