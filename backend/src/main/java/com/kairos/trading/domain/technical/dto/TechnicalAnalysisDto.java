package com.kairos.trading.domain.technical.dto;

/**
 * 기술적 분석 결과 DTO.
 * Vector 에이전트가 반환하는 분석 결과.
 */
public record TechnicalAnalysisDto(
        String stockCode,
        String stockName,
        String pattern, // NanoBanana, Breakout, Bearish, Trap, Neutral
        boolean patternValid,
        long entryPrice, // 진입가
        long targetPrice, // 목표가
        long stopLossPrice, // 손절가
        double maConvergence, // 이평선 수렴도 (0~1)
        double volumeRatio, // 거래량 비율 (전일 대비)
        double orderBookRatio, // 매수잔량/매도잔량 비율
        boolean isFakeWall, // 허매수벽 의심
        int entryScore, // 진입 점수 (0~100)
        String summary // 분석 요약
) {
    /**
     * NanoBanana 패턴인지 확인
     */
    public boolean isNanoBanana() {
        return "NanoBanana".equals(pattern) && patternValid;
    }

    /**
     * Trap 패턴인지 확인
     */
    public boolean isTrap() {
        return "Trap".equals(pattern) || isFakeWall;
    }

    /**
     * 매수 시그널인지 확인
     */
    public boolean hasBuySignal() {
        return isNanoBanana() &&
                !isFakeWall &&
                volumeRatio >= 2.0 &&
                entryScore >= 70;
    }

    /**
     * 예상 수익률 계산
     */
    public double getExpectedReturn() {
        if (entryPrice <= 0)
            return 0;
        return ((double) (targetPrice - entryPrice) / entryPrice) * 100;
    }

    /**
     * 리스크 비율 계산 (손절 대비 수익 비율)
     */
    public double getRiskRewardRatio() {
        if (entryPrice <= 0 || stopLossPrice <= 0)
            return 0;
        double risk = entryPrice - stopLossPrice;
        double reward = targetPrice - entryPrice;
        return risk > 0 ? reward / risk : 0;
    }
}
