package com.kairos.trading.domain.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 정보 엔티티.
 * 실시간 자산 현황 및 예수금을 관리한다.
 */
@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", nullable = false, unique = true, length = 20)
    private String accountNo;

    @Column(name = "total_asset", precision = 18, scale = 2)
    private BigDecimal totalAsset;

    @Column(name = "deposit", precision = 18, scale = 2)
    private BigDecimal deposit;

    @Column(name = "d2_deposit", precision = 18, scale = 2)
    private BigDecimal d2Deposit;

    @Column(name = "daily_profit", precision = 18, scale = 2)
    private BigDecimal dailyProfit;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 예수금을 갱신한다.
     */
    public void updateDeposit(BigDecimal deposit, BigDecimal d2Deposit) {
        this.deposit = deposit;
        this.d2Deposit = d2Deposit;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 자산 정보를 갱신한다.
     */
    public void updateAsset(BigDecimal totalAsset, BigDecimal dailyProfit) {
        this.totalAsset = totalAsset;
        this.dailyProfit = dailyProfit;
        this.updatedAt = LocalDateTime.now();
    }
}
