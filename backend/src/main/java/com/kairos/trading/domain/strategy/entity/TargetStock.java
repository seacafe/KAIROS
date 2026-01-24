package com.kairos.trading.domain.strategy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 타겟 종목 엔티티.
 * 에이전트들의 분석 결과와 전략적 의사결정을 저장한다.
 * TradeLog의 부모 엔티티.
 */
@Entity
@Table(name = "target_stock", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "base_date", "stock_code" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TargetStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "stock_name", length = 50)
    private String stockName;

    // ===== Analysis Scores (Raw Data for AI Review) =====

    @Column(name = "news_score")
    private Integer newsScore; // Sentinel 점수

    @Column(name = "tech_score")
    private Integer techScore; // Vector 점수

    @Column(name = "fund_score")
    private Integer fundScore; // Axiom 점수

    @Column(name = "flow_score")
    private Integer flowScore; // Sonar 점수

    @Column(name = "nexus_score")
    private Integer nexusScore; // 종합 점수

    // ===== Decision =====

    @Column(name = "decision", length = 20)
    private String decision; // BUY, WATCH, REJECT

    @Column(name = "risk_level", length = 10)
    private String riskLevel; // HIGH, MEDIUM, LOW

    @Column(name = "strategy_mode", length = 20)
    private String strategyMode; // 분석 당시 사용자 성향

    // ===== Dynamic Pricing =====

    @Column(name = "original_target_price", precision = 10, scale = 0)
    private BigDecimal originalTargetPrice;

    @Column(name = "original_stop_loss", precision = 10, scale = 0)
    private BigDecimal originalStopLoss;

    @Column(name = "current_target_price", precision = 10, scale = 0)
    private BigDecimal currentTargetPrice;

    @Column(name = "current_stop_loss", precision = 10, scale = 0)
    private BigDecimal currentStopLoss;

    // ===== Status =====

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "WATCHING"; // WATCHING, TRADED, ENDED

    @Column(name = "nexus_reason", columnDefinition = "TEXT")
    private String nexusReason; // AI 선정 사유

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 트레일링 스탑 가격을 갱신한다.
     */
    public void updateTrailingStop(BigDecimal newTargetPrice, BigDecimal newStopLoss) {
        this.currentTargetPrice = newTargetPrice;
        this.currentStopLoss = newStopLoss;
    }

    /**
     * 상태를 변경한다.
     */
    public void changeStatus(String newStatus) {
        this.status = newStatus;
    }

    /**
     * 매수 승인 여부를 반환한다.
     */
    public boolean isBuyApproved() {
        return "BUY".equals(this.decision);
    }
}
