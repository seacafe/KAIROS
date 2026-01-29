package com.kairos.trading.domain.execution.entity;

import com.kairos.trading.domain.strategy.entity.TargetStock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 매매 실행 로그 엔티티.
 * 주문 및 체결 정보를 기록한다.
 */
@Entity
@Table(name = "trade_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TradeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_stock_id")
    private TargetStock targetStock;

    @Column(name = "order_id", length = 20)
    private String orderId; // 키움 주문번호

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "stock_name", length = 50)
    private String stockName;

    // ===== Execution Details =====

    @Column(name = "trade_type", length = 10)
    private String tradeType; // BUY, SELL

    @Column(name = "profit_loss", precision = 15, scale = 0)
    private BigDecimal profitLoss;

    @Column(name = "order_price", precision = 10, scale = 0)
    private BigDecimal orderPrice;

    @Column(name = "filled_price", precision = 10, scale = 0)
    private BigDecimal filledPrice;

    @Column(name = "quantity")
    private Integer quantity;

    // ===== Quality Metrics =====

    @Column(name = "slippage_rate", precision = 5, scale = 2)
    private BigDecimal slippageRate;

    @Column(name = "market_score_snapshot")
    private Integer marketScoreSnapshot;

    // ===== Status =====

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, FILLED, PARTIAL, CANCELLED

    @Column(name = "agent_msg", length = 255)
    private String agentMsg; // Aegis/Vector의 주문 코멘트

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /**
     * 체결 정보를 갱신한다.
     */
    public void fill(BigDecimal filledPrice, String orderId) {
        this.filledPrice = filledPrice;
        this.orderId = orderId;
        this.status = "FILLED";
        this.executedAt = LocalDateTime.now();

        // 슬리피지 계산
        if (this.orderPrice != null && this.orderPrice.compareTo(BigDecimal.ZERO) > 0) {
            var slippage = filledPrice.subtract(this.orderPrice)
                    .divide(this.orderPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            this.slippageRate = slippage.abs();
        }
    }

    /**
     * 부분 체결을 처리한다.
     */
    public void partialFill(int filledQuantity) {
        this.status = "PARTIAL";
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 주문을 취소한다.
     */
    public void cancel(String reason) {
        this.status = "CANCELLED";
        this.agentMsg = reason;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 슬리피지가 기준치 이상인지 확인한다.
     */
    public boolean hasExcessiveSlippage(BigDecimal threshold) {
        return this.slippageRate != null &&
                this.slippageRate.compareTo(threshold) > 0;
    }
}
