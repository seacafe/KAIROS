package com.kairos.trading.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 주식기세(0A) WebSocket 이벤트.
 * Vector 에이전트가 종목의 시가/고가/저가 분석에 활용.
 */
public class StockQuoteEvent extends ApplicationEvent {

    private final String stockCode;
    private final String stockName;
    private final long openPrice; // 시가
    private final long highPrice; // 고가
    private final long lowPrice; // 저가
    private final long currentPrice; // 현재가
    private final long basePrice; // 기준가
    private final double changeRate; // 등락률

    public StockQuoteEvent(Object source, String stockCode, String stockName,
            long openPrice, long highPrice, long lowPrice, long currentPrice,
            long basePrice, double changeRate) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.currentPrice = currentPrice;
        this.basePrice = basePrice;
        this.changeRate = changeRate;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public long getOpenPrice() {
        return openPrice;
    }

    public long getHighPrice() {
        return highPrice;
    }

    public long getLowPrice() {
        return lowPrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public double getChangeRate() {
        return changeRate;
    }

    /**
     * 갭 상승 여부 (시가 > 전일종가 * 1.02).
     */
    public boolean isGapUp() {
        return basePrice > 0 && openPrice > basePrice * 1.02;
    }

    /**
     * 신고가 돌파 여부.
     */
    public boolean isNewHigh() {
        return currentPrice == highPrice && changeRate > 3.0;
    }
}
