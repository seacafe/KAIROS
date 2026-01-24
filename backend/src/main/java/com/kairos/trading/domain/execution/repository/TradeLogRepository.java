package com.kairos.trading.domain.execution.repository;

import com.kairos.trading.domain.execution.entity.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 매매 로그 Repository.
 */
@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    /**
     * 당일 매매 로그 조회.
     */
    @Query("SELECT t FROM TradeLog t WHERE t.executedAt >= :startOfDay ORDER BY t.executedAt DESC")
    List<TradeLog> findTodayLogs(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 종목코드로 매매 로그 조회.
     */
    List<TradeLog> findByStockCodeOrderByExecutedAtDesc(String stockCode);

    /**
     * 특정 기간 매매 로그 조회.
     */
    List<TradeLog> findByExecutedAtBetweenOrderByExecutedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * 슬리피지 과다 발생 거래 조회 (0.5% 이상).
     */
    @Query("SELECT t FROM TradeLog t WHERE t.slippageRate >= 0.5 AND t.executedAt >= :startOfDay")
    List<TradeLog> findHighSlippageTrades(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 당일 종목별 마지막 매매 조회 (재진입 판단용).
     */
    @Query("SELECT t FROM TradeLog t WHERE t.stockCode = :stockCode AND t.executedAt >= :startOfDay ORDER BY t.executedAt DESC")
    List<TradeLog> findLatestByStockCode(@Param("stockCode") String stockCode,
            @Param("startOfDay") LocalDateTime startOfDay);
}
