package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * 주식 호가 응답 (ka10004).
 * Vector 에이전트가 스프레드 계산 및 진입가 결정에 활용.
 */
public record QuoteResponse(
        String stockCode,
        String stockName,
        int currentPrice,
        int priceChange,
        double changeRate,
        long accVolume,
        List<QuoteLevel> askQuotes, // 매도호가 (5단계)
        List<QuoteLevel> bidQuotes // 매수호가 (5단계)
) {
    /**
     * 호가 단계.
     */
    public record QuoteLevel(
            int price,
            long volume,
            int volumeChange) {
    }

    /**
     * 매도/매수 1호가 스프레드 계산.
     */
    public double getSpread() {
        if (askQuotes.isEmpty() || bidQuotes.isEmpty())
            return 0.0;
        int ask1 = askQuotes.get(0).price();
        int bid1 = bidQuotes.get(0).price();
        if (bid1 == 0)
            return 0.0;
        return (double) (ask1 - bid1) / bid1 * 100;
    }

    /**
     * 매수 잔량 vs 매도 잔량 비율.
     * > 1이면 매수세 우위.
     */
    public double getBidAskRatio() {
        long totalAsk = askQuotes.stream().mapToLong(QuoteLevel::volume).sum();
        long totalBid = bidQuotes.stream().mapToLong(QuoteLevel::volume).sum();
        if (totalAsk == 0)
            return 0.0;
        return (double) totalBid / totalAsk;
    }
}
