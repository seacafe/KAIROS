package com.kairos.trading.domain.account.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 계좌 요약 정보 DTO.
 * 잔고 + 보유종목 + 수익률 통합 데이터.
 */
public record AccountSummaryDto(
        // 계좌 기본 정보
        String accountNo,
        BigDecimal totalAsset,
        BigDecimal deposit,
        BigDecimal d2Deposit,

        // 손익 정보
        BigDecimal dailyProfitLoss,
        double dailyReturnRate,
        BigDecimal totalProfitLoss,
        double totalReturnRate,

        // 보유 종목 요약
        int holdingCount,
        List<HoldingDto> holdings,

        // 추가 통계
        BigDecimal maxProfit, // 최대 수익 종목
        BigDecimal maxLoss, // 최대 손실 종목
        String bestPerformer, // 최고 수익 종목명
        String worstPerformer // 최저 수익 종목명
) {
    /**
     * 빌더 패턴을 위한 팩토리 메서드.
     */
    public static AccountSummaryDto of(
            AccountBalanceDto balance,
            List<HoldingDto> holdings) {
        BigDecimal totalProfitLoss = holdings.stream()
                .map(HoldingDto::profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = holdings.stream()
                .map(h -> h.avgPrice().multiply(BigDecimal.valueOf(h.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalReturnRate = 0;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnRate = totalProfitLoss.divide(totalCost, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        HoldingDto best = holdings.stream()
                .max(Comparator.comparingDouble(HoldingDto::profitRate))
                .orElse(null);

        HoldingDto worst = holdings.stream()
                .min(Comparator.comparingDouble(HoldingDto::profitRate))
                .orElse(null);

        return new AccountSummaryDto(
                balance.accountNo(),
                balance.totalAsset(),
                balance.deposit(),
                balance.d2Deposit(),
                balance.dailyProfitLoss(),
                balance.dailyReturnRate(),
                totalProfitLoss,
                totalReturnRate,
                holdings.size(),
                holdings,
                best != null ? best.profitLoss() : BigDecimal.ZERO,
                worst != null ? worst.profitLoss() : BigDecimal.ZERO,
                best != null ? best.stockName() : "",
                worst != null ? worst.stockName() : "");
    }
}
