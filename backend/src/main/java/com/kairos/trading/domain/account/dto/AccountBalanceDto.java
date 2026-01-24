package com.kairos.trading.domain.account.dto;

import java.math.BigDecimal;

/**
 * 계좌 잔고 응답 DTO.
 */
public record AccountBalanceDto(
        String accountNo,
        BigDecimal totalAsset, // 총 자산
        BigDecimal deposit, // 예수금
        BigDecimal d2Deposit, // D+2 예수금
        BigDecimal dailyProfitLoss, // 당일 손익
        double dailyReturnRate // 당일 수익률
) {
    public static AccountBalanceDto of(
            String accountNo,
            BigDecimal totalAsset,
            BigDecimal deposit,
            BigDecimal d2Deposit,
            BigDecimal dailyProfitLoss) {
        double returnRate = 0;
        if (totalAsset != null && totalAsset.compareTo(BigDecimal.ZERO) > 0 && dailyProfitLoss != null) {
            var prevAsset = totalAsset.subtract(dailyProfitLoss);
            if (prevAsset.compareTo(BigDecimal.ZERO) > 0) {
                returnRate = dailyProfitLoss.divide(prevAsset, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
        }
        return new AccountBalanceDto(accountNo, totalAsset, deposit, d2Deposit, dailyProfitLoss, returnRate);
    }
}
