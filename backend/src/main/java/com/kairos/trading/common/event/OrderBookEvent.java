package com.kairos.trading.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 실시간 호가잔량 이벤트 (0D).
 * Vector 에이전트가 호가창 변동 감지에 활용.
 */
@Getter
public class OrderBookEvent extends ApplicationEvent {

    private final String stockCode;
    private final String stockName;
    private final long askPrice1;
    private final long askQty1;
    private final long bidPrice1;
    private final long bidQty1;
    private final long totalAskQty;
    private final long totalBidQty;

    public OrderBookEvent(Object source,
            String stockCode,
            String stockName,
            long askPrice1,
            long askQty1,
            long bidPrice1,
            long bidQty1,
            long totalAskQty,
            long totalBidQty) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.askPrice1 = askPrice1;
        this.askQty1 = askQty1;
        this.bidPrice1 = bidPrice1;
        this.bidQty1 = bidQty1;
        this.totalAskQty = totalAskQty;
        this.totalBidQty = totalBidQty;
    }

    /**
     * 매수/매도 잔량 비율.
     * > 1이면 매수 우위, < 1이면 매도 우위.
     */
    public double getBidAskRatio() {
        if (totalAskQty == 0)
            return 0.0;
        return (double) totalBidQty / totalAskQty;
    }
}
