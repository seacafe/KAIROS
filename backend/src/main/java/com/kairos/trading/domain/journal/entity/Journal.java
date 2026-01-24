package com.kairos.trading.domain.journal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매매 일지 엔티티.
 * 일별 매매 요약 및 AI 복기를 저장한다.
 */
@Entity
@Table(name = "journal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    // ===== Daily Summary =====

    @Column(name = "total_profit_loss", precision = 18, scale = 2)
    private BigDecimal totalProfitLoss;

    @Column(name = "win_rate", precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(name = "trade_count")
    private Integer tradeCount;

    // ===== AI Feedback =====

    @Column(name = "best_trade_log_id")
    private Long bestTradeLogId;

    @Column(name = "worst_trade_log_id")
    private Long worstTradeLogId;

    @Column(name = "ai_review_content", columnDefinition = "TEXT")
    private String aiReviewContent;

    @Column(name = "improvement_points", columnDefinition = "TEXT")
    private String improvementPoints; // JSON 형식

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * AI 복기 내용을 갱신한다.
     */
    public void updateAiReview(String reviewContent, String improvementPoints) {
        this.aiReviewContent = reviewContent;
        this.improvementPoints = improvementPoints;
    }

    /**
     * 일별 통계를 갱신한다.
     */
    public void updateDailyStats(BigDecimal totalProfitLoss, BigDecimal winRate, int tradeCount) {
        this.totalProfitLoss = totalProfitLoss;
        this.winRate = winRate;
        this.tradeCount = tradeCount;
    }

    /**
     * 수익인지 확인한다.
     */
    public boolean isProfitable() {
        return this.totalProfitLoss != null &&
                this.totalProfitLoss.compareTo(BigDecimal.ZERO) > 0;
    }
}
