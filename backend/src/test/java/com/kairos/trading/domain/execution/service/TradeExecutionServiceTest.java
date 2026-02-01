package com.kairos.trading.domain.execution.service;

import com.kairos.trading.common.client.BalanceResponse;
import com.kairos.trading.common.client.KiwoomOrderClient;
import com.kairos.trading.common.client.KiwoomTokenService;
import com.kairos.trading.common.client.OrderResult;
import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.domain.execution.dto.ManualSellRequest;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.PriorityBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeExecutionServiceTest {

    @Mock
    private KiwoomOrderClient orderClient;

    @Mock
    private com.kairos.trading.common.client.KiwoomClient kiwoomClient;

    @Mock
    private KiwoomTokenService tokenService;

    @Mock
    private TradeLogService tradeLogService;

    @InjectMocks
    private TradeExecutionService executionService;

    private PriorityBlockingQueue<ExecutionOrder> orderQueue;

    @BeforeEach
    void setUp() {
        // dry-run 모드를 비활성화하여 실제 API 호출 로직 테스트
        ReflectionTestUtils.setField(executionService, "dryRun", true);

        // private field인 orderQueue에 접근 (상태 확인용)
        orderQueue = (PriorityBlockingQueue<ExecutionOrder>) ReflectionTestUtils.getField(executionService,
                "orderQueue");
    }

    @Test
    @DisplayName("submitOrder: 우선순위 큐에 주문이 정상적으로 추가되어야 한다")
    void submitOrder_ShouldAddOrderToQueue() {
        // Given
        ExecutionOrder buyOrder = ExecutionOrder.newBuy("005930", "삼성전자", 10, new BigDecimal("60000"),
                new BigDecimal("65000"), new BigDecimal("58000"), "LOW", "Test Buy");

        // When
        executionService.submitOrder(buyOrder);

        // Then
        assertThat(orderQueue).hasSize(1);
        assertThat(orderQueue.peek()).isEqualTo(buyOrder);
    }

    @Test
    @DisplayName("onKillSwitch: 이벤트 수신 시 Kill Switch(P0) 주문이 즉시 추가되어야 한다")
    void onKillSwitch_ShouldAddHighPriorityOrder() {
        // Given
        KillSwitchEvent event = new KillSwitchEvent(this, "005930", "삼성전자", "Emergency", "Test");

        // 미리 일반 주문 하나 추가 (우선순위 비교를 위해)
        ExecutionOrder normalOrder = ExecutionOrder.newBuy("005930", "삼성전자", 10, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, "LOW", "Normal");
        executionService.submitOrder(normalOrder);

        // When
        executionService.onKillSwitch(event);

        // Then - processNextOrder()가 호출되어 P0가 처리됨, P2만 남음
        assertThat(orderQueue).hasSize(1);
        assertThat(orderQueue.peek()).isEqualTo(normalOrder);
    }

    @Test
    @DisplayName("executeManualSell: 수동 매도 요청이 Kill Switch 주문으로 변환되어야 한다")
    void executeManualSell_ShouldCreateKillSwitchOrder() {
        // Given
        ManualSellRequest request = new ManualSellRequest("000660", 50, "User Request");

        // When
        executionService.executeManualSell(request);

        // Then - executeManualSell 내부에서 processNextOrder()가 호출되어 큐에서 빠져나감
        assertThat(orderQueue).isEmpty();
    }

    @Test
    @DisplayName("getPendingOrderCount: 대기 중인 주문 개수를 반환해야 한다")
    void getPendingOrderCount_ShouldReturnSize() {
        // Given
        executionService.submitOrder(ExecutionOrder.newBuy("005930", "Sansung", 1, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, "LOW", "1"));
        executionService.submitOrder(ExecutionOrder.newBuy("000660", "Hynix", 1, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, "LOW", "2"));

        // When
        int count = executionService.getPendingOrderCount();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("processNextOrder: 큐가 비어있어도 예외가 발생하지 않아야 한다")
    void processNextOrder_ShouldDoNothing_WhenQueueIsEmpty() {
        // Given
        assertThat(orderQueue).isEmpty();

        // When & Then (No Exception)
        executionService.processNextOrder();
    }

    @Test
    @DisplayName("Priority Queue: 높은 우선순위(P0)가 낮은 우선순위(P2)보다 먼저 처리되어야 한다")
    void orderQueue_ShouldSortByPriority() {
        // Given
        ExecutionOrder lowPriority = ExecutionOrder.newBuy("005930", "Samsung", 1, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, "LOW", "P2");
        ExecutionOrder highPriority = ExecutionOrder.killSwitchSell("000660", "Hynix", 1, "P0");

        // When (순서 상관 없이 넣어도)
        executionService.submitOrder(lowPriority);
        executionService.submitOrder(highPriority);

        // Then (OrderQueue 내부 정렬 확인)
        assertThat(orderQueue.peek()).isEqualTo(highPriority);

        // Polling 순서 확인
        assertThat(orderQueue.poll()).isEqualTo(highPriority);
        assertThat(orderQueue.poll()).isEqualTo(lowPriority);
    }

    @Test
    @DisplayName("processNextOrder: DRY-RUN 모드에서는 실제 API를 호출하지 않아야 한다")
    void processNextOrder_ShouldNotCallApi_WhenDryRun() {
        // Given
        ReflectionTestUtils.setField(executionService, "dryRun", true);

        ExecutionOrder buyOrder = ExecutionOrder.newBuy("005930", "Samsung", 10, new BigDecimal("60000"),
                new BigDecimal("65000"), new BigDecimal("58000"), "LOW", "Test Buy");
        executionService.submitOrder(buyOrder);

        // When
        executionService.processNextOrder();

        // Then - API 호출 없음
        verify(orderClient, never()).submitBuyOrder(any(), any(), anyInt(), anyLong());
        verify(orderClient, never()).getBalance(any());
        assertThat(orderQueue).isEmpty();
    }

    @Test
    @DisplayName("processNextOrder: 실거래 모드에서 BUY 주문 시 예수금 확인 후 주문 전송")
    void processNextOrder_ShouldCheckBalanceAndSubmitOrder_WhenNotDryRun() {
        // Given
        ReflectionTestUtils.setField(executionService, "dryRun", false);

        given(tokenService.getValidToken()).willReturn("test-token");
        given(orderClient.getBalance("test-token"))
                .willReturn(new BalanceResponse(10000000, 10000000, 0, 0, 0.0));
        given(orderClient.submitBuyOrder(eq("test-token"), eq("005930"), eq(10), eq(60000L)))
                .willReturn(new OrderResult("ORD001", "093000", "0", "APBK0013", "주문 완료"));

        ExecutionOrder buyOrder = ExecutionOrder.newBuy("005930", "Samsung", 10, new BigDecimal("60000"),
                new BigDecimal("65000"), new BigDecimal("58000"), "LOW", "Test Buy");
        executionService.submitOrder(buyOrder);

        // When
        executionService.processNextOrder();

        // Then
        verify(tokenService).getValidToken();
        verify(orderClient).getBalance("test-token");
        verify(orderClient).submitBuyOrder("test-token", "005930", 10, 60000L);
        verify(tradeLogService).saveOrderResult(eq(buyOrder), any(OrderResult.class));
    }

    @Test
    @DisplayName("submitOrder: 주문 가격이 음수이거나 StockCode가 없으면 주문이 거부되어야 한다")
    void submitOrder_ShouldReject_WhenPriceIsInvalid() {
        // Given
        ExecutionOrder invalidOrder = ExecutionOrder.newBuy(null, "NoCode", 10, new BigDecimal("-100"),
                BigDecimal.TEN, BigDecimal.TEN, "LOW", "Invalid");

        // When
        executionService.submitOrder(invalidOrder);

        // Then
        assertThat(orderQueue).isEmpty();
    }
}
