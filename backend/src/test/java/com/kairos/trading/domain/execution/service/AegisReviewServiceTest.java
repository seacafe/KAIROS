package com.kairos.trading.domain.execution.service;

import com.kairos.trading.domain.execution.agent.AegisReviewAiClient;
import com.kairos.trading.domain.execution.dto.SlippageAnalysisDto;
import com.kairos.trading.domain.execution.entity.TradeLog;
import com.kairos.trading.domain.execution.repository.TradeLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AegisReviewServiceTest {

        @Mock
        private TradeLogRepository tradeLogRepository;

        @Mock
        private AegisReviewAiClient aegisReviewAiClient;

        @InjectMocks
        private AegisReviewService aegisReviewService;

        @Test
        @DisplayName("analyzeSlippage: 슬리피지가 높은 거래가 있으면 AI 분석을 수행해야 한다")
        void analyzeSlippage_ShouldCallAi() {
                // Given
                TradeLog highSlippageTrade = TradeLog.builder()
                                .id(1L)
                                .stockName("급등주")
                                .orderPrice(new BigDecimal("10000"))
                                .filledPrice(new BigDecimal("10200")) // 2% miss
                                .slippageRate(new BigDecimal("2.0"))
                                .build();

                given(tradeLogRepository.findHighSlippageTrades(any(LocalDateTime.class)))
                                .willReturn(List.of(highSlippageTrade));

                // Constructor: long tradeId, long orderPrice, long filledPrice, double
                // slippageRate, String cause, String causeDetail, int timeOffsetMs, int
                // tickOffset, String suggestion
                given(aegisReviewAiClient.analyzeSlippage(anyString(), anyString()))
                                .willReturn(new SlippageAnalysisDto(
                                                1L, 10000L, 10200L, 2.0,
                                                "ORDER_DELAY", "호가 공백으로 인한 체결 지연", 100, 0, "Suggestion"));

                // When
                List<SlippageAnalysisDto> result = aegisReviewService.analyzeSlippage("OrderBook Dump...");

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).cause()).isEqualTo("ORDER_DELAY");
                assertThat(result.get(0).causeDetail()).contains("호가 공백"); // "ORDER_DELAY" or "호가 공백..." ? Record has
                                                                           // 'cause'
                                                                           // field.
                // Wait, 'cause' in DTO: String cause, String causeDetail.
                // My DTO creation: cause="ORDER_DELAY", causeDetail="호가 공백..."
                // Assert: result.get(0).cause() is "ORDER_DELAY".
                // Let's match the assert to the value.
                assertThat(result.get(0).cause()).isEqualTo("ORDER_DELAY");

                // Also check causeDetail if needed
                assertThat(result.get(0).causeDetail()).contains("호가 공백");

                assertThat(result.get(0).timeOffsetMs()).isEqualTo(100);
        }

        @Test
        @DisplayName("analyzeSlippage: 대상 거래가 없으면 빈 리스트를 반환해야 한다")
        void analyzeSlippage_ShouldReturnEmpty_WhenNoTrades() {
                // Given
                given(tradeLogRepository.findHighSlippageTrades(any(LocalDateTime.class)))
                                .willReturn(List.of());

                // When
                List<SlippageAnalysisDto> result = aegisReviewService.analyzeSlippage("Dump");

                // Then
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getTodayStats: 거래 통계를 정확히 계산해야 한다")
        void getTodayStats_ShouldCalculateCorrectly() {
                // Given
                TradeLog win = TradeLog.builder()
                                .tradeType("SELL")
                                .profitLoss(new BigDecimal("5000"))
                                .slippageRate(new BigDecimal("0.1"))
                                .build();

                TradeLog loss = TradeLog.builder()
                                .tradeType("SELL")
                                .profitLoss(new BigDecimal("-2000"))
                                .slippageRate(new BigDecimal("0.5"))
                                .build();

                given(tradeLogRepository.findTodayLogs(any(LocalDateTime.class)))
                                .willReturn(List.of(win, loss));

                // When
                AegisReviewService.TradeStats stats = aegisReviewService.getTodayStats();

                // Then
                assertThat(stats.totalTrades()).isEqualTo(2);
                assertThat(stats.wins()).isEqualTo(1);
                assertThat(stats.winRate()).isEqualTo(50.0);
                assertThat(stats.avgSlippage()).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.001));
        }
}
