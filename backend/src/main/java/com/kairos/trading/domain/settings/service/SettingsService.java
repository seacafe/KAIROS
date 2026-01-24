package com.kairos.trading.domain.settings.service;

import com.kairos.trading.domain.settings.dto.CreateRssFeedRequest;
import com.kairos.trading.domain.settings.dto.RssFeedDto;
import com.kairos.trading.domain.settings.dto.UserSettingDto;
import com.kairos.trading.domain.settings.entity.RssFeed;
import com.kairos.trading.domain.settings.entity.UserSetting;
import com.kairos.trading.domain.settings.repository.RssFeedRepository;
import com.kairos.trading.domain.settings.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 설정 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private final UserSettingRepository userSettingRepository;
    private final RssFeedRepository rssFeedRepository;

    /**
     * 사용자 설정 조회.
     */
    public UserSettingDto getUserSettings() {
        return userSettingRepository.findByUserId("default")
                .map(this::toUserSettingDto)
                .orElseGet(() -> new UserSettingDto("NEUTRAL", true, 3.0));
    }

    /**
     * 투자 성향 변경.
     */
    @Transactional
    public UserSettingDto updateStrategyMode(String mode) {
        UserSetting setting = userSettingRepository.findByUserId("default")
                .orElse(UserSetting.builder().userId("default").build());

        setting.updateStrategyMode(mode);
        userSettingRepository.save(setting);

        log.info("[SettingsService] 투자 성향 변경: {}", mode);
        return toUserSettingDto(setting);
    }

    /**
     * RSS 피드 목록 조회.
     */
    public List<RssFeedDto> getRssFeeds() {
        return rssFeedRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(this::toRssFeedDto)
                .toList();
    }

    /**
     * RSS 피드 추가.
     */
    @Transactional
    public RssFeedDto createRssFeed(CreateRssFeedRequest request) {
        RssFeed feed = RssFeed.builder()
                .name(request.name())
                .url(request.url())
                .category(request.category())
                .isActive(true)
                .build();

        RssFeed saved = rssFeedRepository.save(feed);
        log.info("[SettingsService] RSS 피드 추가: {} ({})", request.name(), request.category());

        return toRssFeedDto(saved);
    }

    /**
     * RSS 피드 삭제.
     */
    @Transactional
    public void deleteRssFeed(Long id) {
        rssFeedRepository.deleteById(id);
        log.info("[SettingsService] RSS 피드 삭제: {}", id);
    }

    private UserSettingDto toUserSettingDto(UserSetting setting) {
        return new UserSettingDto(
                setting.getStrategyMode(),
                setting.getReEntryAllowed(),
                setting.getMaxLossPerTrade());
    }

    private RssFeedDto toRssFeedDto(RssFeed feed) {
        return new RssFeedDto(
                feed.getId(),
                feed.getName(),
                feed.getUrl(),
                feed.getCategory(),
                feed.getIsActive());
    }
}
