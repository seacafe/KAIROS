package com.kairos.trading.domain.execution.service;

import com.kairos.trading.common.client.BalanceResponse;
import com.kairos.trading.common.client.KiwoomOrderClient;
import com.kairos.trading.common.client.KiwoomTokenService;
import com.kairos.trading.common.client.OrderResult;
import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.common.exception.BusinessException;
import com.kairos.trading.common.response.ErrorCode;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Aegis - 매매 실행 서비스.
 * 
 * Dual Mode:
 * 1. Runtime (Java): 예수금 확인, 주문 전송 (AI 없음, 1ms 내 처리)
 * 2. Post-time (AI): 슬리피지 분석, 매매 회고
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final KiwoomOrderClient orderClient;
    private final KiwoomTokenService tokenService;
    private final TradeLogService tradeLogService;

    @Value("${kairos.trading.dry-run:true}")
    private boolean dryRun; // true: 실제 주문 전송 안함 (시뮬레이션)

    // 우선순위 큐 (P0: Kill Switch, P1: 익절, P2: 신규매수)
    private final PriorityBlockingQueue<ExecutionOrder> orderQueue = new PriorityBlockingQueue<>(100,
            (o1, o2) -> Integer.compare(o1.priority(), o2.priority()));

    /**
     * Kill Switch 이벤트 수신.
     * 즉시 시장가 매도를 큐에 추가한다.
     */
    @EventListener
    public void onKillSwitch(KillSwitchEvent event) {
        log.error("[Aegis] ⚠️ KILL SWITCH 수신: {} - {}", event.getStockName(), event.getReason());

        // TODO: 실제 보유 수량 조회 필요
        var order = ExecutionOrder.killSwitchSell(
                event.getStockCode(),
                event.getStockName(),
                0, // 전량 매도 (수량은 실시간 조회)
                event.getReason());

        orderQueue.offer(order);
        log.info("[Aegis] Kill Switch 주문 추가: {}", order);

        // 즉시 처리
        processNextOrder();
    }

    /**
     * 주문을 큐에 추가한다.
     */
    public void submitOrder(ExecutionOrder order) {
        if (order.stockCode() == null || order.stockCode().isBlank() ||
                order.quantity() <= 0 ||
                (order.entryPrice() != null && order.entryPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            log.warn("[Aegis] ❌ 잘못된 주문 요청 거부: {}", order);
            return;
        }

        log.info("[Aegis] 주문 접수: {} {} {} @ {}",
                order.action(), order.stockName(), order.quantity(), order.entryPrice());

        orderQueue.offer(order);
    }

    /**
     * 큐의 다음 주문을 처리한다.
     */
    public void processNextOrder() {
        var order = orderQueue.poll();
        if (order == null) {
            return;
        }

        try {
            executeOrderInternal(order);
        } catch (Exception e) {
            log.error("[Aegis] ⚠️ 주문 처리 중 예외 발생 (System not halted): {} - {}", order.stockCode(), e.getMessage());
        }
    }

    /**
     * 실제 주문 실행 로직.
     */
    protected void executeOrderInternal(ExecutionOrder order) {
        log.info("[Aegis] 주문 처리 시작: P{} {} {}",
                order.priority(), order.action(), order.stockCode());

        // Dry-run 모드 체크
        if (dryRun) {
            log.warn("[Aegis] 🔵 DRY-RUN 모드 - 실제 주문 전송 안함: {} {} {}주 @ {}",
                    order.action(), order.stockCode(), order.quantity(), order.entryPrice());
            return;
        }

        try {
            // 1. 토큰 확인/발급
            String token = tokenService.getValidToken();

            // 2. 예수금 확인 (매수 시에만)
            if ("BUY".equals(order.action())) {
                BalanceResponse balance = orderClient.getBalance(token);
                long requiredAmount = order.entryPrice().longValue() * order.quantity();

                if (!balance.canAfford(requiredAmount)) {
                    log.error("[Aegis] ❌ 예수금 부족: 필요={}, 가용={}", requiredAmount, balance.availableAmount());
                    throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
                }
                log.info("[Aegis] 예수금 확인 완료: 가용={}", balance.availableAmount());
            }

            // 3. 주문 전송
            OrderResult result = executeOrder(token, order);

            // 4. 결과 로깅 및 TradeLog 저장
            if (result.isSuccess()) {
                log.info("[Aegis] ✅ 주문 성공: {} {} {}주 @ {} (주문번호: {})",
                        order.action(), order.stockCode(), order.quantity(),
                        order.entryPrice(), result.orderId());

                tradeLogService.saveOrderResult(order, result);
            } else {
                log.error("[Aegis] ❌ 주문 실패: {}", result.message());
            }

        } catch (BusinessException e) {
            log.error("[Aegis] 주문 처리 실패: {} - {}", e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Aegis] 주문 처리 중 예외 발생", e);
            throw new BusinessException(ErrorCode.ORDER_FAILED, e);
        }
    }

    /**
     * 주문 유형에 따른 API 호출.
     */
    private OrderResult executeOrder(String token, ExecutionOrder order) {
        return switch (order.action()) {
            case "BUY" -> orderClient.submitBuyOrder(
                    token,
                    order.stockCode(),
                    order.quantity(),
                    order.entryPrice().longValue());
            case "SELL" -> {
                if (order.isKillSwitch()) {
                    log.warn("[Aegis] 🔴 Kill Switch 시장가 매도 실행: {}", order.stockName());
                    yield orderClient.submitMarketSellOrder(token, order.stockCode(), order.quantity());
                } else {
                    yield orderClient.submitSellOrder(
                            token,
                            order.stockCode(),
                            order.quantity(),
                            order.entryPrice().longValue());
                }
            }
            default -> throw new IllegalStateException("Unknown action: " + order.action());
        };
    }

    /**
     * 큐에 대기 중인 주문 수를 반환한다.
     */
    public int getPendingOrderCount() {
        return orderQueue.size();
    }

    /**
     * 수동 매도 주문을 처리한다.
     */
    public void executeManualSell(com.kairos.trading.domain.execution.dto.ManualSellRequest request) {
        log.warn("[Aegis] 수동 매도 요청 처리: {} {}주", request.stockCode(), request.quantity());

        var order = ExecutionOrder.killSwitchSell(
                request.stockCode(),
                request.stockCode(), // TODO: 종목명 조회
                request.quantity(),
                "수동 매도: " + (request.reason() != null ? request.reason() : "사용자 요청"));

        submitOrder(order);
        processNextOrder();
    }
}
