package com.kairos.trading.common.event;

import java.time.LocalDateTime;

/**
 * 실시간 체결 데이터 이벤트.
 * WebSocket `00` 메시지 수신 시 발행된다.
 */
public class TickDataEvent extends org.springframework.context.ApplicationEvent {

    private final String stockCode;
    private final long price;
    private final long volume;
    private final long accVolume; // 누적 거래량
    private final double changeRate; // 등락률
    private final LocalDateTime timestamp;

    public TickDataEvent(
            Object source,
            String stockCode,
            long price,
            long volume,
            long accVolume,
            double changeRate) {
        super(source);
        this.stockCode = stockCode;
        this.price = price;
        this.volume = volume;
        this.accVolume = accVolume;
        this.changeRate = changeRate;
        this.timestamp = LocalDateTime.now();
    }

    public String getStockCode() {
        return stockCode;
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

    public double getChangeRate() {
        return changeRate;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
