package com.kairos.trading.domain.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 트레일링 스탑 서비스.
 * ATR 기반 동적 손절가 조정.
 */
@Slf4j
@Service
public class TrailingStopService {

    // 트레일링 스탑 시작 수익률 (%)
    private static final double TRAILING_START_PERCENT = 3.0;

    // 트레일링 비율 (고점 대비 %)
    private static final double TRAILING_RATIO = 0.5;

    /**
     * 트레일링 스탑 손절가 계산.
     * 
     * @param originalStopLoss 원래 손절가
     * @param currentPrice     현재가
     * @param targetPrice      목표가
     * @return 새로운 손절가 (원래보다 높아야만 반환)
     */
    public long calculateTrailingStop(long originalStopLoss, long currentPrice, long targetPrice) {
        // 원래 진입가 역산 (대략적)
        long estimatedEntry = (originalStopLoss + targetPrice) / 2;

        // 현재 수익률
        double profitRate = ((double) (currentPrice - estimatedEntry) / estimatedEntry) * 100;

        // 수익률이 TRAILING_START_PERCENT 미만이면 원래 손절가 유지
        if (profitRate < TRAILING_START_PERCENT) {
            return originalStopLoss;
        }

        // 트레일링 스탑: 현재가의 TRAILING_RATIO% 아래
        long gap = targetPrice - estimatedEntry;
        long trailingDistance = (long) (gap * TRAILING_RATIO);
        long newStopLoss = currentPrice - trailingDistance;

        // 새로운 손절가가 원래보다 높을 때만 적용
        return Math.max(originalStopLoss, newStopLoss);
    }

    /**
     * ATR 기반 트레일링 스탑 계산.
     * 
     * @param atr          Average True Range
     * @param currentPrice 현재가
     * @param multiplier   ATR 배수 (기본 2.0)
     * @return 새로운 손절가
     */
    public long calculateAtrTrailingStop(double atr, long currentPrice, double multiplier) {
        long atrStop = currentPrice - (long) (atr * multiplier);
        log.debug("[TrailingStop] ATR 기반: 현재가 {} - (ATR {} × {}) = {}",
                currentPrice, atr, multiplier, atrStop);
        return atrStop;
    }
}
