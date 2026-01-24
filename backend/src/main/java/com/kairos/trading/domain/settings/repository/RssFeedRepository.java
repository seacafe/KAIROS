package com.kairos.trading.domain.settings.repository;

import com.kairos.trading.domain.settings.entity.RssFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RSS 피드 Repository.
 */
@Repository
public interface RssFeedRepository extends JpaRepository<RssFeed, Long> {

    /**
     * 활성화된 RSS 피드 목록 조회.
     */
    List<RssFeed> findByIsActiveTrueOrderByIdAsc();

    /**
     * 카테고리별 RSS 피드 조회.
     */
    List<RssFeed> findByCategoryAndIsActiveTrue(String category);
}
