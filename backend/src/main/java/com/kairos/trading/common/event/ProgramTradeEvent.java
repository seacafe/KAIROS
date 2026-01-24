package com.kairos.trading.common.event;

import java.time.LocalDateTime;

/**
 * 프로그램 매매 이벤트.
 * WebSocket `0w` 메시지 수신 시 발행된다.
 */
public class ProgramTradeEvent extends org.springframework.context.ApplicationEvent {

    private final String stockCode;
    private final long programBuy; // 프로그램 매수 금액
    private final long programSell; // 프로그램 매도 금액
    private final long programNet; // 프로그램 순매수
    private final LocalDateTime timestamp;

    public ProgramTradeEvent(
            Object source,
            String stockCode,
            long programBuy,
            long programSell) {
        super(source);
        this.stockCode = stockCode;
        this.programBuy = programBuy;
        this.programSell = programSell;
        this.programNet = programBuy - programSell;
        this.timestamp = LocalDateTime.now();
    }

    public String getStockCode() {
        return stockCode;
    }

    public long getProgramBuy() {
        return programBuy;
    }

    public long getProgramSell() {
        return programSell;
    }

    public long getProgramNet() {
        return programNet;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 프로그램 매도 우세인지 확인 (설거지 패턴 감지용)
     */
    public boolean isDistributionPattern() {
        return programNet < -10_000_000_000L; // 100억 이상 순매도
    }
}
