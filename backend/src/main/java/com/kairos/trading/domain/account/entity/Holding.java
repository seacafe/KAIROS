package com.kairos.trading.domain.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 보유 종목 엔티티.
 */
@Entity
@Table(name = "holding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "avg_price", precision = 18, scale = 2)
    private BigDecimal avgPrice;

    @Column(name = "current_price", precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "profit_loss", precision = 18, scale = 2)
    private BigDecimal profitLoss;

    @Column(name = "profit_rate")
    private Double profitRate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 현재가 및 손익 갱신.
     */
    public void updatePrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        if (this.avgPrice != null && currentPrice != null && this.quantity != null) {
            this.profitLoss = currentPrice.subtract(this.avgPrice)
                    .multiply(BigDecimal.valueOf(this.quantity));
            this.profitRate = currentPrice.subtract(this.avgPrice)
                    .divide(this.avgPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수량 변경.
     */
    public void updateQuantity(int quantity, BigDecimal avgPrice) {
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.updatedAt = LocalDateTime.now();
    }
}
