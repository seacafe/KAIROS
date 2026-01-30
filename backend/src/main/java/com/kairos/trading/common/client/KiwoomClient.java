package com.kairos.trading.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.gateway.ApiType;
import com.kairos.trading.common.response.ErrorCode;
import com.kairos.trading.domain.fundamental.dto.StockInfoResponse;
import com.kairos.trading.domain.news.dto.OAuthTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

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
}
