package com.kairos.trading.domain.sentiment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 투자심리 분석 엔티티.
 * Resonance 에이전트가 분석한 시장 심리 및 공포-탐욕 지수 저장.
 */
@Entity
@Table(name = "sentiment_analyses")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SentimentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 분석 대상 (종목코드 또는 'MARKET' for 전체 시장) */
    @Column(nullable = false, length = 10)
    private String target;

    /** 분석 일시 */
    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    /** Resonance 점수 (0-100) */
    @Column(nullable = false)
    private Integer score;

    /** 투자 결정 (BUY, WATCH, REJECT, ALERT) */
    @Column(nullable = false, length = 10)
    private String decision;

    /** 분석 사유 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    // === 심리 지표 ===

    /** 공포-탐욕 지수 (0-100, 0=극단적 공포, 100=극단적 탐욕) */
    private Integer fearGreedIndex;

    /** VIX 지수 (변동성 지수) */
    private BigDecimal vixIndex;

    /** 풋-콜 비율 */
    private BigDecimal putCallRatio;

    /** 신용융자 잔고 변화율 (%) */
    private BigDecimal marginBalanceChange;

    /** 개인 순매수 (억원) */
    private BigDecimal retailNetBuy;

    /** 외국인 순매수 (억원) */
    private BigDecimal foreignNetBuy;

    /** 기관 순매수 (억원) */
    private BigDecimal institutionNetBuy;

    // === 메타데이터 ===

    /** 생성일시 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
