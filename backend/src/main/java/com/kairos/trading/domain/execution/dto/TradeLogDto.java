package com.kairos.trading.domain.execution.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 매매 로그 응답 DTO.
 */
public record TradeLogDto(
        Long id,
        String stockCode,
        String stockName,
        String tradeType, // BUY, SELL
        BigDecimal orderPrice,
        BigDecimal filledPrice,
        int quantity,
        BigDecimal slippageRate,
        String status, // PENDING, FILLED, CANCELLED
        String agentMsg,
        LocalDateTime executedAt) {
}
