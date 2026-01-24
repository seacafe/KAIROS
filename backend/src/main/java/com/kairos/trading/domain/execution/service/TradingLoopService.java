package com.kairos.trading.domain.execution.service;

import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.common.event.TickDataEvent;
import com.kairos.trading.common.event.ViEvent;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import com.kairos.trading.domain.strategy.entity.TargetStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 매매 루프 서비스.
 * 
 * 체결가 수신 → 목표가/손절가 도달 확인 → 주문 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingLoopService {

    private final TradeExecutionService executionService;
    private final TrailingStopService trailingStopService;
    private final ApplicationEventPublisher eventPublisher;

    // 실시간 모니터링 중인 종목 (종목코드 → TargetStock)
    private final Map<String, TargetStock> activeTargets = new ConcurrentHashMap<>();

    // 현재가 캐시
    private final Map<String, Long> currentPrices = new ConcurrentHashMap<>();

    /**
     * 모니터링 대상 종목 등록.
     */
    public void registerTarget(TargetStock target) {
        activeTargets.put(target.getStockCode(), target);
        log.info("[TradingLoop] 모니터링 등록: {} (목표: {}, 손절: {})",
                target.getStockName(), target.getCurrentTargetPrice(), target.getCurrentStopLoss());
    }

    /**
     * 모니터링 대상 종목 해제.
     */
    public void unregisterTarget(String stockCode) {
        activeTargets.remove(stockCode);
        currentPrices.remove(stockCode);
        log.info("[TradingLoop] 모니터링 해제: {}", stockCode);
    }

    /**
     * 체결가 이벤트 수신.
     */
    @EventListener
    public void onTickData(TickDataEvent event) {
        var stockCode = event.getStockCode();
        var price = event.getPrice();

        // 현재가 캐시 업데이트
        currentPrices.put(stockCode, price);

        // 모니터링 중인 종목인지 확인
        var target = activeTargets.get(stockCode);
        if (target == null) {
            return;
        }

        // 가격 체크
        checkPriceConditions(target, price);
    }

    /**
     * VI 이벤트 수신 → Kill Switch 발동.
     */
    @EventListener
    public void onViEvent(ViEvent event) {
        if (!event.requiresKillSwitch()) {
            return;
        }

        var stockCode = event.getStockCode();
        var target = activeTargets.get(stockCode);

        if (target != null) {
            log.error("[TradingLoop] 🚨 정적 VI 발동! Kill Switch 실행: {}", event.getStockName());

            eventPublisher.publishEvent(new KillSwitchEvent(
                    this,
                    stockCode,
                    event.getStockName(),
                    "정적 VI 발동 @ " + event.getTriggerPrice(),
                    "TradingLoop"));

            unregisterTarget(stockCode);
        }
    }

    /**
     * 가격 조건 확인 (목표가/손절가 도달).
     */
    private void checkPriceConditions(TargetStock target, long currentPrice) {
        var targetPrice = target.getCurrentTargetPrice();
        var stopLoss = target.getCurrentStopLoss();

        // 1. 목표가 도달 → 익절
        if (targetPrice != null && currentPrice >= targetPrice.longValue()) {
            log.info("[TradingLoop] 🎯 목표가 도달! {} @ {} (목표: {})",
                    target.getStockName(), currentPrice, targetPrice);

            var order = ExecutionOrder.profitTake(
                    target.getStockCode(),
                    target.getStockName(),
                    0, // 전량 매도
                    BigDecimal.valueOf(currentPrice));
            executionService.submitOrder(order);
            unregisterTarget(target.getStockCode());
            return;
        }

        // 2. 손절가 도달 → 손절
        if (stopLoss != null && currentPrice <= stopLoss.longValue()) {
            log.warn("[TradingLoop] ⛔ 손절가 도달! {} @ {} (손절: {})",
                    target.getStockName(), currentPrice, stopLoss);

            var order = ExecutionOrder.killSwitchSell(
                    target.getStockCode(),
                    target.getStockName(),
                    0,
                    "손절가 도달 @ " + currentPrice);
            executionService.submitOrder(order);
            unregisterTarget(target.getStockCode());
            return;
        }

        // 3. 트레일링 스탑 업데이트
        if (targetPrice != null) {
            var newStopLoss = trailingStopService.calculateTrailingStop(
                    target.getOriginalStopLoss().longValue(),
                    currentPrice,
                    target.getOriginalTargetPrice().longValue());

            if (newStopLoss > stopLoss.longValue()) {
                target.updateTrailingStop(targetPrice, BigDecimal.valueOf(newStopLoss));
                log.debug("[TradingLoop] 트레일링 스탑 업데이트: {} → {}",
                        target.getStockName(), newStopLoss);
            }
        }
    }

    /**
     * 모니터링 중인 종목 수.
     */
    public int getActiveTargetCount() {
        return activeTargets.size();
    }

    /**
     * 특정 종목 현재가 조회.
     */
    public Long getCurrentPrice(String stockCode) {
        return currentPrices.get(stockCode);
    }
}
