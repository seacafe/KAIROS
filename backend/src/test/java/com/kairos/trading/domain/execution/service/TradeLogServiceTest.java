package com.kairos.trading.domain.execution.service;

import com.kairos.trading.domain.execution.dto.TradeLogDto;
import com.kairos.trading.domain.execution.entity.TradeLog;
import com.kairos.trading.domain.execution.mapper.ExecutionMapper;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TradeLogServiceTest {

        @Mock
        private TradeLogRepository tradeLogRepository;

        @Mock
        private ExecutionMapper executionMapper;

        @InjectMocks
        private TradeLogService tradeLogService;

        @Test
        @DisplayName("getTodayLogs: 당일 거래 로그를 조회하고 DTO로 변환해 반환해야 한다")
        void getTodayLogs_ShouldReturnDtoList() {
                // Given
                TradeLog logEntity = TradeLog.builder()
                                .id(1L)
                                .stockCode("005930")
                                .stockName("삼성전자")
                                .orderPrice(new BigDecimal("70000"))
                                .build();

                // Correct Constructor: Long, String, String, String, BigDecimal, BigDecimal,
                // int, BigDecimal, String, String, LocalDateTime
                TradeLogDto logDto = new TradeLogDto(
                                1L, "005930", "삼성전자", "BUY",
                                new BigDecimal("70000"), new BigDecimal("70000"), 10,
                                BigDecimal.ZERO, "EXECUTION", "MSG", LocalDateTime.now());

                given(tradeLogRepository.findTodayLogs(any(LocalDateTime.class))).willReturn(List.of(logEntity));
                given(executionMapper.toTradeLogDtoList(List.of(logEntity))).willReturn(List.of(logDto));

                // When
                List<TradeLogDto> result = tradeLogService.getTodayLogs();

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).stockName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("getHighSlippageTrades: 슬리피지 과다 거래를 조회해야 한다")
        void getHighSlippageTrades_ShouldReturnList() {
                // Given
                TradeLog logEntity = TradeLog.builder()
                                .id(2L)
                                .slippageRate(new BigDecimal("1.5"))
                                .build();

                TradeLogDto logDto = new TradeLogDto(
                                2L, "000660", "SK하이닉스", "SELL",
                                new BigDecimal("100000"), new BigDecimal("98500"), 10,
                                new BigDecimal("1.5"), "SLIPPAGE", "MSG", LocalDateTime.now());

                given(tradeLogRepository.findHighSlippageTrades(any(LocalDateTime.class)))
                                .willReturn(List.of(logEntity));
                given(executionMapper.toTradeLogDtoList(List.of(logEntity))).willReturn(List.of(logDto));

                // When
                List<TradeLogDto> result = tradeLogService.getHighSlippageTrades();

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).slippageRate()).isEqualTo(new BigDecimal("1.5"));
        }
}
