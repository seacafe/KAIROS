package com.kairos.trading.domain.sentiment.repository;

import com.kairos.trading.domain.sentiment.entity.SentimentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 투자심리 분석 리포지토리.
 */
@Repository
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long> {

    /**
     * 대상(종목/시장)별 최신 분석 조회.
     */
    Optional<SentimentAnalysis> findTopByTargetOrderByAnalyzedAtDesc(String target);

    /**
     * 시장 전체 최신 심리 분석 조회.
     */
    default Optional<SentimentAnalysis> findLatestMarketSentiment() {
        return findTopByTargetOrderByAnalyzedAtDesc("MARKET");
    }

    /**
     * 특정 기간 내 분석 목록 조회.
     */
    List<SentimentAnalysis> findByAnalyzedAtBetweenOrderByAnalyzedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * 공포-탐욕 지수 기준 조회 (극단적 공포 = 30 미만).
     */
    List<SentimentAnalysis> findByFearGreedIndexLessThanOrderByAnalyzedAtDesc(int threshold);

    /**
     * 공포-탐욕 지수 기준 조회 (극단적 탐욕 = 70 초과).
     */
    List<SentimentAnalysis> findByFearGreedIndexGreaterThanOrderByAnalyzedAtDesc(int threshold);
}
