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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kairos.trading.domain.account.dto.AccountEvaluationResponse;
import com.kairos.trading.domain.account.dto.ExecutionBalanceResponse;

/**
 * 키움증권 Open API 주문 클라이언트.
 *
 * 역할:
 * 1. 예수금 조회 (kt00004)
 * 2. 매수 주문 전송 (kt10000)
 * 3. 매도 주문 전송 (kt10001)
 * 4. 시장가 매도 (Kill Switch)
 */
@Slf4j
@Component
public class KiwoomOrderClient {

    private final ApiGatekeeper gatekeeper;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String appKey;
    private final String appSecret;
    private final String accountNo;
    private final boolean isVirtual; // app-key prefix로 자동 감지

    // 주문 구분 코드
    private static final String ORDER_TYPE_LIMIT = "00"; // 지정가
    private static final String ORDER_TYPE_MARKET = "01"; // 시장가

    // 매매 구분 코드
    private static final String TRADE_TYPE_SELL = "01"; // 매도
    private static final String TRADE_TYPE_BUY = "02"; // 매수

    public KiwoomOrderClient(ApiGatekeeper gatekeeper,
            @Value("${kiwoom.api.base-url}") String baseUrl,
            @Value("${kiwoom.api.app-key}") String appKey,
            @Value("${kiwoom.api.app-secret}") String appSecret,
            @Value("${kiwoom.api.account-no:}") String accountNo,
            @Value("${kiwoom.api.is-virtual:true}") boolean isVirtual) {
        this.gatekeeper = gatekeeper;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.accountNo = accountNo;
        this.isVirtual = isVirtual;
        this.objectMapper = new ObjectMapper();

        // 타임아웃 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();

        log.info("[KiwoomOrderClient] 초기화 완료 - 모의투자 모드: {}", this.isVirtual);
    }

    /**
     * 예수금 조회 (kt00004).
     */
    public BalanceResponse getBalance(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 예수금 조회 요청");
            try {
                var response = restClient.get()
                        .uri("/api/dostk/acntinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("appkey", appKey)
                        .header("appsecret", appSecret)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            log.error("[KiwoomOrder] 예수금 조회 실패: {} - {}", res.getStatusCode(), res.getStatusText());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "예수금 조회 실패");
                        })
                        .body(String.class);

                return parseBalanceResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 예수금 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 매수 주문 전송 (kt10000).
     */
    public OrderResult submitBuyOrder(String token, String stockCode, int quantity, long price) {
        return submitOrder(token, stockCode, quantity, price, TRADE_TYPE_BUY, ORDER_TYPE_LIMIT);
    }

    /**
     * 매도 주문 전송 (kt10001).
     */
    public OrderResult submitSellOrder(String token, String stockCode, int quantity, long price) {
        return submitOrder(token, stockCode, quantity, price, TRADE_TYPE_SELL, ORDER_TYPE_LIMIT);
    }

    /**
     * 시장가 매도 주문 (Kill Switch용).
     */
    public OrderResult submitMarketSellOrder(String token, String stockCode, int quantity) {
        return submitOrder(token, stockCode, quantity, 0, TRADE_TYPE_SELL, ORDER_TYPE_MARKET);
    }

    /**
     * 내부 주문 전송 메서드.
     */
    private OrderResult submitOrder(String token, String stockCode, int quantity,
            long price, String tradeType, String orderType) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 주문 전송: {} {} {}주 @ {}",
                    "02".equals(tradeType) ? "매수" : "매도", stockCode, quantity, price);
            try {
                // 요청 본문 구성
                Map<String, Object> body = new HashMap<>();
                body.put("pdno", stockCode); // 종목코드
                body.put("ord_dvsn", orderType); // 주문구분 (00: 지정가, 01: 시장가)
                body.put("sll_buy_dvsn_cd", tradeType); // 매매구분 (01: 매도, 02: 매수)
                body.put("ord_qty", String.valueOf(quantity));
                body.put("ord_unpr", String.valueOf(price));
                body.put("cano", accountNo.split("-")[0]); // 계좌번호 앞 8자리
                body.put("acnt_prdt_cd", accountNo.split("-").length > 1 ? accountNo.split("-")[1] : "01"); // 상품코드

                var response = restClient.post()
                        .uri("/api/dostk/order")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("appkey", appKey)
                        .header("appsecret", appSecret)
                        .header("tr_id", isVirtual ? getVirtualTrId(tradeType) : getRealTrId(tradeType))
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            log.warn("[KiwoomOrder] Rate Limit 초과");
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            log.error("[KiwoomOrder] 주문 실패: {} - {}", res.getStatusCode(), res.getStatusText());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "주문 전송 실패");
                        })
                        .body(String.class);

                return parseOrderResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 주문 처리 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    // 모의투자 TR ID
    private String getVirtualTrId(String tradeType) {
        return "02".equals(tradeType) ? "VTTC0802U" : "VTTC0801U"; // 매수/매도
    }

    // 실전투자 TR ID
    private String getRealTrId(String tradeType) {
        return "02".equals(tradeType) ? "TTTC0802U" : "TTTC0801U"; // 매수/매도
    }

    private BalanceResponse parseBalanceResponse(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return new BalanceResponse(
                node.path("ord_psbl_cash").asLong(),
                node.path("tot_evlu_amt").asLong(),
                node.path("pchs_amt_smtl").asLong(),
                node.path("evlu_pfls_smtl").asLong(),
                node.path("evlu_pfls_rt").asDouble());
    }

    private OrderResult parseOrderResponse(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        String rtCd = node.path("rt_cd").asText();

        if (!"0".equals(rtCd)) {
            // 주문 실패
            String msgCd = node.path("msg_cd").asText();
            String msg = node.path("msg1").asText();
            log.error("[KiwoomOrder] 주문 거부: {} - {}", msgCd, msg);
            throw new BusinessException(ErrorCode.KIWOOM_ORDER_FAILED, msg);
        }

        JsonNode output = node.path("output");
        return new OrderResult(
                output.path("ord_no").asText(),
                output.path("ord_tmd").asText(),
                rtCd,
                node.path("msg_cd").asText(),
                node.path("msg1").asText());
    }

    /**
     * 주문 정정 (kt10001 - Amend).
     * 
     * @param token           접근 토큰
     * @param originalOrderNo 원주문번호
     * @param stockCode       종목코드
     * @param quantity        정정 수량
     * @param price           정정 가격
     * @return 정정 결과
     */
    public OrderResult amendOrder(String token, String originalOrderNo,
            String stockCode, int quantity, long price) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 주문 정정: 원주문={}, {} {}주 @ {}",
                    originalOrderNo, stockCode, quantity, price);
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("cano", accountNo.split("-")[0]);
                body.put("acnt_prdt_cd", accountNo.split("-").length > 1 ? accountNo.split("-")[1] : "01");
                body.put("orgn_odno", originalOrderNo); // 원주문번호
                body.put("pdno", stockCode);
                body.put("ord_dvsn", ORDER_TYPE_LIMIT);
                body.put("rvse_cncl_dvsn_cd", "01"); // 정정취소구분 (01: 정정)
                body.put("ord_qty", String.valueOf(quantity));
                body.put("ord_unpr", String.valueOf(price));
                body.put("qty_all_ord_yn", "N"); // 전량주문여부

                var response = restClient.post()
                        .uri("/api/dostk/order")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("appkey", appKey)
                        .header("appsecret", appSecret)
                        .header("tr_id", isVirtual ? "VTTC0803U" : "TTTC0803U") // 정정
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "주문 정정 실패");
                        })
                        .body(String.class);

                return parseOrderResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 주문 정정 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    /**
     * 주문 취소 (kt10003 - Cancel).
     * 
     * @param token           접근 토큰
     * @param originalOrderNo 원주문번호
     * @param stockCode       종목코드
     * @param quantity        취소 수량 (0이면 전량)
     * @return 취소 결과
     */
    public OrderResult cancelOrder(String token, String originalOrderNo,
            String stockCode, int quantity) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 주문 취소: 원주문={}, {} {}주",
                    originalOrderNo, stockCode, quantity == 0 ? "전량" : quantity);
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("cano", accountNo.split("-")[0]);
                body.put("acnt_prdt_cd", accountNo.split("-").length > 1 ? accountNo.split("-")[1] : "01");
                body.put("orgn_odno", originalOrderNo);
                body.put("pdno", stockCode);
                body.put("ord_dvsn", ORDER_TYPE_LIMIT);
                body.put("rvse_cncl_dvsn_cd", "02"); // 정정취소구분 (02: 취소)
                body.put("ord_qty", String.valueOf(quantity));
                body.put("ord_unpr", "0"); // 취소시 가격 불필요
                body.put("qty_all_ord_yn", quantity == 0 ? "Y" : "N"); // 전량 취소

                var response = restClient.post()
                        .uri("/api/dostk/order")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("appkey", appKey)
                        .header("appsecret", appSecret)
                        .header("tr_id", isVirtual ? "VTTC0803U" : "TTTC0803U") // 정정/취소 동일
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_RATE_LIMIT_EXCEEDED);
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "주문 취소 실패");
                        })
                        .body(String.class);

                return parseOrderResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 주문 취소 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    // ========== 계좌 평가 현황 (kt00004) ==========

    /**
     * 계좌 평가 현황 조회.
     * 예수금, 손익, 보유종목 리스트 포함.
     * 
     * @param token 접근 토큰
     * @return 계좌 평가 응답
     */
    public AccountEvaluationResponse getAccountEvaluation(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 계좌 평가 현황 조회");
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("cano", accountNo.split("-")[0]);
                body.put("acnt_prdt_cd", accountNo.split("-").length > 1 ? accountNo.split("-")[1] : "01");
                body.put("qry_tp", "0"); // 전체조회
                body.put("dmst_stex_tp", "KRX");

                var response = restClient.post()
                        .uri("/api/dostk/acntinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("appkey", appKey)
                        .header("appsecret", appSecret)
                        .header("tr_id", isVirtual ? "VTTC8434R" : "TTTC8434R")
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "계좌 평가 조회 실패");
                        })
                        .body(String.class);

                return parseAccountEvaluationResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 계좌 평가 조회 예외", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private AccountEvaluationResponse parseAccountEvaluationResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);

        List<AccountEvaluationResponse.HoldingStock> holdings = new ArrayList<>();
        JsonNode stocksNode = root.path("stk_acnt_evlt_prst");
        if (stocksNode.isArray()) {
            for (JsonNode stock : stocksNode) {
                holdings.add(new AccountEvaluationResponse.HoldingStock(
                        stock.path("stk_cd").asText(),
                        stock.path("stk_nm").asText(),
                        stock.path("rmnd_qty").asInt(),
                        stock.path("avg_prc").asInt(),
                        stock.path("cur_prc").asInt(),
                        stock.path("evlt_amt").asLong(),
                        stock.path("pl_amt").asLong(),
                        stock.path("pl_rt").asDouble(),
                        stock.path("pur_amt").asLong(),
                        stock.path("tdy_buyq").asInt(),
                        stock.path("tdy_sellq").asInt()));
            }
        }

        return new AccountEvaluationResponse(
                root.path("acnt_nm").asText(),
                root.path("entr").asLong(),
                root.path("d2_entra").asLong(),
                root.path("tot_est_amt").asLong(),
                root.path("tot_pur_amt").asLong(),
                root.path("tdy_lspft").asLong(),
                root.path("tdy_lspft_rt").asDouble(),
                root.path("lspft").asLong(),
                root.path("lspft_rt").asDouble(),
                holdings);
    }

    // ========== 체결 잔고 (kt00005) ==========

    /**
     * 체결 잔고 조회.
     * 예수금 상세 및 종목별 체결잔고 포함.
     * 
     * @param token 접근 토큰
     * @return 체결 잔고 응답
     */
    public ExecutionBalanceResponse getExecutionBalance(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 체결 잔고 조회");
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("cano", accountNo.split("-")[0]);
                body.put("acnt_prdt_cd", accountNo.split("-").length > 1 ? accountNo.split("-")[1] : "01");
                body.put("dmst_stex_tp", "KRX");

                var response = restClient.post()
                        .uri("/api/dostk/acntinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("appkey", appKey)
                        .header("appsecret", appSecret)
                        .header("tr_id", isVirtual ? "VTTC8498R" : "TTTC8498R")
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, "체결잔고 조회 실패");
                        })
                        .body(String.class);

                return parseExecutionBalanceResponse(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 체결 잔고 조회 예외", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private ExecutionBalanceResponse parseExecutionBalanceResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);

        List<ExecutionBalanceResponse.ExecutionStock> stocks = new ArrayList<>();
        JsonNode stocksNode = root.path("stk_cntr_remn");
        if (stocksNode.isArray()) {
            for (JsonNode stock : stocksNode) {
                stocks.add(new ExecutionBalanceResponse.ExecutionStock(
                        stock.path("stk_cd").asText(),
                        stock.path("stk_nm").asText(),
                        stock.path("setl_remn").asInt(),
                        stock.path("cur_qty").asInt(),
                        stock.path("cur_prc").asInt(),
                        stock.path("buy_uv").asInt(),
                        stock.path("pur_amt").asLong(),
                        stock.path("evlt_amt").asLong(),
                        stock.path("evltv_prft").asLong(),
                        stock.path("pl_rt").asDouble(),
                        stock.path("crd_tp").asText(),
                        stock.path("loan_dt").asText(),
                        stock.path("expr_dt").asText()));
            }
        }

        return new ExecutionBalanceResponse(
                root.path("entr").asLong(),
                root.path("entr_d1").asLong(),
                root.path("entr_d2").asLong(),
                root.path("pymn_alow_amt").asLong(),
                root.path("ord_alowa").asLong(),
                root.path("stk_buy_tot_amt").asLong(),
                root.path("evlt_amt_tot").asLong(),
                root.path("tot_pl_tot").asLong(),
                root.path("tot_pl_rt").asDouble(),
                stocks);
    }

    // ========== 미체결 조회 (kt10002) ==========

    /**
     * 미체결 주문 조회.
     * 
     * @param token 접근 토큰
     * @return 미체결 주문 리스트
     */
    public List<UnfilledOrderDto> getUnfilledOrders(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 미체결 조회");
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("cano", accountNo.split("-")[0]);
                body.put("ctno", accountNo.split("-")[1]);

                var response = restClient.post()
                        .uri("/api/dostk/order")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header("tr_id", "kt10002")
                        .body(body)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            log.error("[KiwoomOrder] 미체결 조회 실패: {}", res.getStatusCode());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR);
                        })
                        .body(String.class);

                return parseUnfilledOrders(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 미체결 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private List<UnfilledOrderDto> parseUnfilledOrders(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode output = root.path("output");

        List<UnfilledOrderDto> orders = new ArrayList<>();
        if (output.isArray()) {
            for (JsonNode item : output) {
                orders.add(new UnfilledOrderDto(
                        item.path("orgn_ord_no").asText(),
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("buy_sl_tp").asText().equals("01") ? "BUY" : "SELL",
                        item.path("ord_qty").asInt(),
                        item.path("ord_pric").asLong(),
                        item.path("not_ccld_qty").asInt(),
                        item.path("ccld_qty").asInt(),
                        item.path("ord_dt").asText(),
                        item.path("ord_tm").asText()));
            }
        }
        return orders;
    }

    /**
     * 미체결 주문 DTO.
     */
    public record UnfilledOrderDto(
            String orderNo,
            String stockCode,
            String stockName,
            String orderType,
            int orderQty,
            long orderPrice,
            int unfilledQty,
            int filledQty,
            String orderDate,
            String orderTime) {
    }

    // ========== 당일 체결 내역 (kt10003) ==========

    /**
     * 당일 체결 내역 조회.
     * 
     * @param token 접근 토큰
     * @return 체결 내역 리스트
     */
    public List<ExecutedOrderDto> getExecutedOrders(String token) {
        return gatekeeper.execute(ApiType.KIWOOM, () -> {
            log.info("[KiwoomOrder] 당일 체결 내역 조회");
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("cano", accountNo.split("-")[0]);
                body.put("ctno", accountNo.split("-")[1]);

                var response = restClient.post()
                        .uri("/api/dostk/order")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header("tr_id", "kt10003")
                        .body(body)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            log.error("[KiwoomOrder] 체결 내역 조회 실패: {}", res.getStatusCode());
                            throw new BusinessException(ErrorCode.KIWOOM_API_ERROR);
                        })
                        .body(String.class);

                return parseExecutedOrders(response);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[KiwoomOrder] 체결 내역 조회 중 예외 발생", e);
                throw new BusinessException(ErrorCode.KIWOOM_API_ERROR, e);
            }
        });
    }

    private List<ExecutedOrderDto> parseExecutedOrders(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode output = root.path("output");

        List<ExecutedOrderDto> orders = new ArrayList<>();
        if (output.isArray()) {
            for (JsonNode item : output) {
                orders.add(new ExecutedOrderDto(
                        item.path("ord_no").asText(),
                        item.path("stk_cd").asText(),
                        item.path("stk_nm").asText(),
                        item.path("buy_sl_tp").asText().equals("01") ? "BUY" : "SELL",
                        item.path("ccld_qty").asInt(),
                        item.path("ccld_pric").asLong(),
                        item.path("ccld_amt").asLong(),
                        item.path("ccld_dt").asText(),
                        item.path("ccld_tm").asText()));
            }
        }
        return orders;
    }

    /**
     * 당일 체결 DTO.
     */
    public record ExecutedOrderDto(
            String orderNo,
            String stockCode,
            String stockName,
            String orderType,
            int filledQty,
            long filledPrice,
            long filledAmount,
            String filledDate,
            String filledTime) {
    }
}
