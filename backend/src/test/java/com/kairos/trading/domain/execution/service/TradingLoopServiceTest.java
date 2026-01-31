package com.kairos.trading.domain.execution.service;

import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.common.event.TickDataEvent;
import com.kairos.trading.common.event.ViEvent;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import com.kairos.trading.domain.strategy.entity.TargetStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradingLoopServiceTest {

    @Mock
    private TradeExecutionService executionService;

    @Mock
    private TrailingStopService trailingStopService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TradingLoopService tradingLoopService;

    @Test
    @DisplayName("registerTarget: 종목 등록 시 모니터링 목록에 추가되어야 한다")
    void registerTarget_ShouldAddToActiveTargets() {
        // Given
        TargetStock target = TargetStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentTargetPrice(new BigDecimal("70000"))
                .currentStopLoss(new BigDecimal("60000"))
                .build();

        // When
        tradingLoopService.registerTarget(target);

        // Then
        assertThat(tradingLoopService.getActiveTargetCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("onTickData: 목표가 도달 시 익절 주문을 생성하고 모니터링을 해제해야 한다")
    void onTickData_ShouldSubmitProfitOrder_WhenTargetReached() {
        // Given
        TargetStock target = TargetStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentTargetPrice(new BigDecimal("70000"))
                .currentStopLoss(new BigDecimal("60000"))
                .build();
        tradingLoopService.registerTarget(target);

        TickDataEvent event = new TickDataEvent(this, "005930", 71000, 100, 1000, 1.5);

        // When
        tradingLoopService.onTickData(event);

        // Then
        ArgumentCaptor<ExecutionOrder> orderCaptor = ArgumentCaptor.forClass(ExecutionOrder.class);
        verify(executionService).submitOrder(orderCaptor.capture());

        ExecutionOrder order = orderCaptor.getValue();
        assertThat(order.action()).isEqualTo("SELL");
        assertThat(order.reason()).contains("목표가 도달");

        assertThat(tradingLoopService.getActiveTargetCount()).isZero(); // 모니터링 해제 확인
    }

    @Test
    @DisplayName("onTickData: 손절가 도달 시 손절 주문을 생성하고 모니터링을 해제해야 한다")
    void onTickData_ShouldSubmitStopLossOrder_WhenStopLossReached() {
        // Given
        TargetStock target = TargetStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentTargetPrice(new BigDecimal("70000"))
                .currentStopLoss(new BigDecimal("60000"))
                .build();
        tradingLoopService.registerTarget(target);

        TickDataEvent event = new TickDataEvent(this, "005930", 59000, 100, 1000, -2.0);

        // When
        tradingLoopService.onTickData(event);

        // Then
        ArgumentCaptor<ExecutionOrder> orderCaptor = ArgumentCaptor.forClass(ExecutionOrder.class);
        verify(executionService).submitOrder(orderCaptor.capture());

        ExecutionOrder order = orderCaptor.getValue();
        assertThat(order.action()).isEqualTo("SELL"); // Kill Switch Sell (P0)
        assertThat(order.priority()).isZero(); // Priority 0 check
        assertThat(order.reason()).contains("손절가 도달");

        assertThat(tradingLoopService.getActiveTargetCount()).isZero();
    }

    @Test
    @DisplayName("onViEvent: 정적 VI 발동 시 Kill Switch 이벤트를 발행해야 한다")
    void onViEvent_ShouldPublishKillSwitch_WhenViActivated() {
        // Given
        TargetStock target = TargetStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .build();
        tradingLoopService.registerTarget(target);

        ViEvent viEvent = new ViEvent(this, "005930", "삼성전자", "STATIC", 65000);

        // When
        tradingLoopService.onViEvent(viEvent);

        // Then
        verify(eventPublisher).publishEvent(any(KillSwitchEvent.class));
        assertThat(tradingLoopService.getActiveTargetCount()).isZero(); // VI 발동 시 즉시 모니터링 중단
    }

    @Test
    @DisplayName("onTickData: 트레일링 스탑 조건 만족 시 손절가를 상향해야 한다")
    void onTickData_ShouldUpdateTrailingStop_WhenConditionMet() {
        // Given
        TargetStock target = TargetStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .originalTargetPrice(new BigDecimal("80000"))
                .originalStopLoss(new BigDecimal("60000"))
                .currentTargetPrice(new BigDecimal("80000"))
                .currentStopLoss(new BigDecimal("60000")) // 초기 손절가
                .build();
        tradingLoopService.registerTarget(target);

        long currentPrice = 75000;
        long newStopLoss = 65000; // 예상되는 새로운 손절가

        // Mock TrailingStopService
        when(trailingStopService.calculateTrailingStop(anyLong(), anyLong(), anyLong()))
                .thenReturn(newStopLoss);

        TickDataEvent event = new TickDataEvent(this, "005930", currentPrice, 100, 1000, 5.0);

        // When
        tradingLoopService.onTickData(event);

        // Then
        // target 객체의 상태가 변경되었는지 확인 (손절가가 65000으로 올랐는지)
        assertThat(target.getCurrentStopLoss()).isEqualByComparingTo(new BigDecimal(newStopLoss));

        // 주문은 발생하지 않아야 함
        verify(executionService, never()).submitOrder(any());
    }
}
