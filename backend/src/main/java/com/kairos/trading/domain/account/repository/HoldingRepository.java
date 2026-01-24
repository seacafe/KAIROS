package com.kairos.trading.domain.account.repository;

import com.kairos.trading.domain.account.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 보유종목 Repository.
 */
@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    /**
     * 계좌 ID로 보유종목 목록 조회.
     */
    List<Holding> findByAccountId(Long accountId);

    /**
     * 계좌 ID와 종목코드로 보유종목 조회.
     */
    java.util.Optional<Holding> findByAccountIdAndStockCode(Long accountId, String stockCode);

    /**
     * 종목코드로 보유종목 조회.
     */
    java.util.Optional<Holding> findByStockCode(String stockCode);
}
