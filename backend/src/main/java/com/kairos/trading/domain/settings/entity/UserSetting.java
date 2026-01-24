package com.kairos.trading.domain.settings.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 설정 엔티티.
 */
@Entity
@Table(name = "user_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    @Builder.Default
    private String userId = "default";

    @Column(name = "strategy_mode", length = 20)
    @Builder.Default
    private String strategyMode = "NEUTRAL"; // AGGRESSIVE, NEUTRAL, STABLE

    @Column(name = "re_entry_allowed")
    @Builder.Default
    private Boolean reEntryAllowed = true;

    @Column(name = "max_loss_per_trade")
    @Builder.Default
    private Double maxLossPerTrade = 3.0; // 3%

    @Column(name = "max_position_count")
    @Builder.Default
    private Integer maxPositionCount = 5;

    @Column(name = "max_position_ratio")
    @Builder.Default
    private Double maxPositionRatio = 0.2; // 20%

    /**
     * 전략 모드 변경.
     */
    public void updateStrategyMode(String mode) {
        if ("AGGRESSIVE".equals(mode) || "NEUTRAL".equals(mode) || "STABLE".equals(mode)) {
            this.strategyMode = mode;
        }
    }
}
