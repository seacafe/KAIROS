package com.kairos.trading.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 주식체결(0B) WebSocket 이벤트.
 * Vector 에이전트가 실시간 체결 데이터 분석에 활용.
 */
public class StockTradeEvent extends ApplicationEvent {

    private final String stockCode;
    private final String stockName;
    private final long price; // 체결가
    private final long volume; // 체결수량
    private final long accVolume; // 누적거래량
    private final long accAmount; // 누적거래대금
    private final String tradeTime; // 체결시간 (HHmmss)
    private final String tradeType; // 매도/매수 구분

    public StockTradeEvent(Object source, String stockCode, String stockName,
            long price, long volume, long accVolume, long accAmount,
            String tradeTime, String tradeType) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.price = price;
        this.volume = volume;
        this.accVolume = accVolume;
        this.accAmount = accAmount;
        this.tradeTime = tradeTime;
        this.tradeType = tradeType;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public long getPrice() {
        return price;
    }

    public long getVolume() {
        return volume;
    }

    public long getAccVolume() {
        return accVolume;
    }

    public long getAccAmount() {
        return accAmount;
    }

    public String getTradeTime() {
        return tradeTime;
    }

    public String getTradeType() {
        return tradeType;
    }

    /**
     * 대량 체결 여부 (1억원 이상).
     */
    public boolean isLargeTrade() {
        return price * volume >= 100_000_000;
    }

    /**
     * 매수 체결인지 여부.
     */
    public boolean isBuySide() {
        return "B".equals(tradeType) || "1".equals(tradeType);
    }
}
