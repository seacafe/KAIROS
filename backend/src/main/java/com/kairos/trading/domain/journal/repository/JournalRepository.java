package com.kairos.trading.domain.journal.repository;

import com.kairos.trading.domain.journal.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 매매일지 Repository.
 */
@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {

    /**
     * 날짜로 매매일지 조회.
     */
    Optional<Journal> findByDate(LocalDate date);

    /**
     * 기간 내 매매일지 목록 조회.
     */
    List<Journal> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * 최근 N일 매매일지 조회.
     */
    List<Journal> findTop30ByOrderByDateDesc();
}
