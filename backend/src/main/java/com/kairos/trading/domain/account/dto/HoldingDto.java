package com.kairos.trading.domain.account.dto;

import java.math.BigDecimal;

/**
 * 보유 종목 DTO.
 */
public record HoldingDto(
        String stockCode,
        String stockName,
        int quantity, // 보유 수량
        BigDecimal avgPrice, // 평균 단가
        BigDecimal currentPrice, // 현재가
        BigDecimal profitLoss, // 평가 손익
        double profitRate, // 수익률
        double weight // 비중
) {
}
