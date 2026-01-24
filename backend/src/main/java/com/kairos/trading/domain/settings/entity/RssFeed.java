package com.kairos.trading.domain.settings.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * RSS 피드 설정 엔티티.
 */
@Entity
@Table(name = "rss_feed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RssFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "category", length = 20)
    private String category; // DOMESTIC, DISCLOSURE, GLOBAL

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "polling_interval_sec")
    @Builder.Default
    private Integer pollingIntervalSec = 60;

    /**
     * 활성화/비활성화 토글.
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}
