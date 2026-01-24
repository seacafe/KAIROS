package com.kairos.trading.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.gateway.ApiType;
import com.kairos.trading.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 키움증권 REST API 클라이언트.
 * 
 * 모든 API 호출은 ApiGatekeeper를 경유하여 Rate Limit을 준수한다.
 * RestClient(동기)를 사용하여 Virtual Thread 환경에서 효율적으로 동작한다.
 * 
 * @see ApiGatekeeper
 */
@Slf4j
@Component
public class KiwoomClient {

    private final ApiGatekeeper gatekeeper;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiId;
    private final String apiSecret;

    public KiwoomClient(
            ApiGatekeeper gatekeeper,
            @Value("${kiwoom.api.base-url:https://api.kiwoom.com}") String baseUrl,
            @Value("${kiwoom.api.id:}") String apiId,
            @Value("${kiwoom.api.secret:}") String apiSecret) {
        this.gatekeeper = gatekeeper;
        this.apiId = apiId;
        this.apiSecret = apiSecret;
        this.objectMapper = new ObjectMapper();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("api-id", apiId)
                .build();

        log.info("KiwoomClient 초기화 완료 - baseUrl: {}", baseUrl);
    }

    /**
     * OAuth2 토큰 발급 (au10001).
     * 장 시작 전 1회 호출하여 토큰을 발급받는다.
     * 
     * @return 발급된 토큰 정보
     * @throws BusinessException 인증 실패 시
     */
    public KiwoomTokenResponse issueToken() {
        log.info("키움 API 토큰 발급 요청");

        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            try {
                var response = restClient.post()
                        .uri("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("grant_type=client_credentials&client_id=" + apiId + "&client_secret=" + apiSecret)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            log.error("토큰 발급 실패: {} - {}", res.getStatusCode(), res.getStatusText());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "토큰 발급 실패");
                        })
                        .body(String.class);

                var node = objectMapper.readTree(response);
                return new KiwoomTokenResponse(
                        node.path("token").asText(),
                        node.path("token_type").asText(),
                        node.path("expires_in").asLong());
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("토큰 발급 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 주식 기본정보 조회 (ka10001).
     * 
     * @param stockCode 종목 코드 (예: "005930")
     * @param token     OAuth2 Bearer 토큰
     * @return 주식 기본정보
     * @throws BusinessException API 호출 실패 시
     */
    public StockInfoResponse getStockInfo(String stockCode, String token) {
        log.debug("주식 기본정보 조회 요청: {}", stockCode);

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
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
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
}
