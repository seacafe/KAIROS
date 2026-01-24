package com.kairos.trading.domain.execution.service;

import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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
public class TradeExecutionService {

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

        log.info("[Aegis] 주문 처리 시작: P{} {} {}",
                order.priority(), order.action(), order.stockCode());

        // TODO: 실제 주문 로직 구현
        // 1. 예수금 확인 (kt00004)
        // 2. 호가 조회 (0C)
        // 3. 주문 전송 (kt10000/kt10001)
        // 4. TradeLog 저장

        if (order.isKillSwitch()) {
            log.warn("[Aegis] 🔴 Kill Switch 시장가 매도 실행: {}", order.stockName());
        } else if ("BUY".equals(order.action())) {
            log.info("[Aegis] 🟢 매수 주문 실행: {} {} @ {}",
                    order.stockName(), order.quantity(), order.entryPrice());
        } else {
            log.info("[Aegis] 🟡 매도 주문 실행: {} {} @ {}",
                    order.stockName(), order.quantity(), order.entryPrice());
        }
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
