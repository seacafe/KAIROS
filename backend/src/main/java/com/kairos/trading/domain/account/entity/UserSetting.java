package com.kairos.trading.domain.account.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 설정 엔티티.
 * 투자 성향 및 매매 파라미터를 관리한다.
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

    @Column(name = "strategy_mode", length = 20)
    @Builder.Default
    private String strategyMode = "NEUTRAL";

    @Column(name = "re_entry_allowed")
    @Builder.Default
    private Boolean reEntryAllowed = true;

    @Column(name = "max_loss_per_trade")
    @Builder.Default
    private Double maxLossPerTrade = 3.0;

    @Column(name = "api_rate_limit_config", columnDefinition = "TEXT")
    private String apiRateLimitConfig;

    /**
     * 투자 성향을 변경한다.
     * 
     * @param strategyMode AGGRESSIVE, NEUTRAL, STABLE 중 하나
     */
    public void changeStrategyMode(String strategyMode) {
        if (!strategyMode.matches("AGGRESSIVE|NEUTRAL|STABLE")) {
            throw new IllegalArgumentException("유효하지 않은 전략 모드: " + strategyMode);
        }
        this.strategyMode = strategyMode;
    }

    /**
     * 재진입 허용 여부를 설정한다.
     */
    public void setReEntryAllowed(boolean allowed) {
        this.reEntryAllowed = allowed;
    }

    /**
     * 최대 손실률을 설정한다.
     * 
     * @param maxLoss 최대 손실률 (0.0 ~ 100.0)
     */
    public void setMaxLossPerTrade(double maxLoss) {
        if (maxLoss < 0 || maxLoss > 100) {
            throw new IllegalArgumentException("최대 손실률은 0~100 사이여야 합니다.");
        }
        this.maxLossPerTrade = maxLoss;
    }
}
