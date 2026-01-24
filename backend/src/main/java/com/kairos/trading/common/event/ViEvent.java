package com.kairos.trading.common.event;

import java.time.LocalDateTime;

/**
 * VI(변동성 완화장치) 발동 이벤트.
 * WebSocket `1h` 메시지 수신 시 발행된다.
 * VI 발동 시 Kill Switch 연동.
 */
public class ViEvent extends org.springframework.context.ApplicationEvent {

    private final String stockCode;
    private final String stockName;
    private final String viType; // STATIC, DYNAMIC
    private final long triggerPrice; // 발동 가격
    private final LocalDateTime timestamp;

    public ViEvent(
            Object source,
            String stockCode,
            String stockName,
            String viType,
            long triggerPrice) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.viType = viType;
        this.triggerPrice = triggerPrice;
        this.timestamp = LocalDateTime.now();
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public String getViType() {
        return viType;
    }

    public long getTriggerPrice() {
        return triggerPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 정적 VI인지 확인 (더 심각한 상황)
     */
    public boolean isStaticVi() {
        return "STATIC".equals(viType);
    }

    /**
     * Kill Switch 발동이 필요한지 확인
     * 정적 VI는 즉시 매도 권고
     */
    public boolean requiresKillSwitch() {
        return isStaticVi();
    }
}
