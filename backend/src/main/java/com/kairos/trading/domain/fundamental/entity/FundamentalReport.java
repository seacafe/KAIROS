package com.kairos.trading.domain.fundamental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 펀더멘털 분석 리포트 엔티티.
 * Axiom 에이전트가 분석한 종목별 펀더멘털 리포트 저장.
 */
@Entity
@Table(name = "fundamental_reports")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FundamentalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 종목코드 */
    @Column(nullable = false, length = 10)
    private String stockCode;

    /** 종목명 */
    @Column(nullable = false, length = 50)
    private String stockName;

    /** 분석 일시 */
    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    /** Axiom 점수 (0-100) */
    @Column(nullable = false)
    private Integer score;

    /** 투자 결정 (BUY, WATCH, REJECT) */
    @Column(nullable = false, length = 10)
    private String decision;

    /** 분석 사유 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    // === 재무 지표 ===

    /** PER (주가수익비율) */
    private BigDecimal per;

    /** PBR (주가순자산비율) */
    private BigDecimal pbr;

    /** ROE (자기자본이익률) */
    private BigDecimal roe;

    /** 부채비율 (%) */
    private BigDecimal debtRatio;

    /** 영업이익률 (%) */
    private BigDecimal operatingMargin;

    /** 매출 성장률 (YoY %) */
    private BigDecimal revenueGrowth;

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
