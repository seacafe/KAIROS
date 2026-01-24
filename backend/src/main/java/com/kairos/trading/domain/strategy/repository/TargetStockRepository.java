package com.kairos.trading.domain.strategy.repository;

import com.kairos.trading.domain.strategy.entity.TargetStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 타겟 종목 Repository.
 */
@Repository
public interface TargetStockRepository extends JpaRepository<TargetStock, Long> {

    /**
     * 특정 날짜의 타겟 종목 조회.
     */
    List<TargetStock> findByBaseDateOrderByNexusScoreDesc(LocalDate baseDate);

    /**
     * 특정 날짜의 BUY 판정 종목만 조회.
     */
    @Query("SELECT t FROM TargetStock t WHERE t.baseDate = :date AND t.decision = 'BUY' ORDER BY t.nexusScore DESC")
    List<TargetStock> findBuyTargets(@Param("date") LocalDate date);

    /**
     * 종목코드와 날짜로 조회.
     */
    Optional<TargetStock> findByBaseDateAndStockCode(LocalDate baseDate, String stockCode);

    /**
     * 특정 날짜의 WATCHING 상태 종목 조회.
     */
    @Query("SELECT t FROM TargetStock t WHERE t.baseDate = :date AND t.status = 'WATCHING'")
    List<TargetStock> findWatchingTargets(@Param("date") LocalDate date);
}
