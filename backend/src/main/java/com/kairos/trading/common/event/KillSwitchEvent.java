package com.kairos.trading.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * Kill Switch 이벤트.
 * Sentinel이 위기 상황 감지 시 발행하며, Aegis가 구독하여 즉시 매도한다.
 */
public class KillSwitchEvent extends ApplicationEvent {

    private final String stockCode;
    private final String stockName;
    private final String reason;
    private final String triggeredBy;

    public KillSwitchEvent(Object source, String stockCode, String stockName, String reason, String triggeredBy) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.reason = reason;
        this.triggeredBy = triggeredBy;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public String getReason() {
        return reason;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    @Override
    public String toString() {
        return String.format("KillSwitchEvent[%s(%s) by %s: %s]",
                stockName, stockCode, triggeredBy, reason);
    }
}
