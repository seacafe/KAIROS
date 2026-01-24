package com.kairos.trading.domain.news.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * RSS 피드 설정 엔티티.
 * Sentinel 에이전트가 모니터링할 RSS 소스를 관리한다.
 */
@Entity
@Table(name = "rss_feed_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RssFeedConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "url", nullable = false, unique = true, length = 255)
    private String url;

    @Column(name = "category", length = 20)
    private String category; // DOMESTIC, DISCLOSURE, GLOBAL

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 피드를 활성화/비활성화한다.
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * 공시 피드인지 확인한다.
     */
    public boolean isDisclosureFeed() {
        return "DISCLOSURE".equals(this.category);
    }

    /**
     * 글로벌 피드인지 확인한다.
     */
    public boolean isGlobalFeed() {
        return "GLOBAL".equals(this.category);
    }
}
