package com.kairos.trading.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 실시간 잔고 업데이트 이벤트 (04).
 * 체결 후 잔고 자동 동기화.
 */
@Getter
public class BalanceUpdateEvent extends ApplicationEvent {

    private final String accountNo;
    private final String stockCode;
    private final String stockName;
    private final int holdQty;
    private final long avgPrice;
    private final long currentPrice;
    private final long evalAmount;
    private final long pnlAmount;
    private final double pnlRate;

    public BalanceUpdateEvent(Object source,
            String accountNo,
            String stockCode,
            String stockName,
            int holdQty,
            long avgPrice,
            long currentPrice,
            long evalAmount,
            long pnlAmount,
            double pnlRate) {
        super(source);
        this.accountNo = accountNo;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.holdQty = holdQty;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.evalAmount = evalAmount;
        this.pnlAmount = pnlAmount;
        this.pnlRate = pnlRate;
    }
}
