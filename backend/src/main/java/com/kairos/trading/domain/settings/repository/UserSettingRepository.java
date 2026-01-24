package com.kairos.trading.domain.settings.repository;

import com.kairos.trading.domain.settings.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 설정 Repository.
 */
@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    /**
     * 사용자 ID로 설정 조회.
     */
    Optional<UserSetting> findByUserId(String userId);
}
