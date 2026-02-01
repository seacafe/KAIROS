package com.kairos.trading.domain.fundamental.repository;

import com.kairos.trading.domain.fundamental.entity.FundamentalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 펀더멘털 리포트 리포지토리.
 */
@Repository
public interface FundamentalReportRepository extends JpaRepository<FundamentalReport, Long> {

    /**
     * 종목코드로 최신 리포트 조회.
     */
    Optional<FundamentalReport> findTopByStockCodeOrderByAnalyzedAtDesc(String stockCode);

    /**
     * 종목코드로 리포트 목록 조회 (최신순).
     */
    List<FundamentalReport> findByStockCodeOrderByAnalyzedAtDesc(String stockCode);

    /**
     * 특정 기간 내 분석된 리포트 조회.
     */
    List<FundamentalReport> findByAnalyzedAtBetweenOrderByAnalyzedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * 특정 점수 이상 리포트 조회.
     */
    List<FundamentalReport> findByScoreGreaterThanEqualOrderByScoreDesc(int minScore);
}
