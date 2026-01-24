package com.kairos.trading.domain.settings.controller;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.domain.settings.dto.CreateRssFeedRequest;
import com.kairos.trading.domain.settings.dto.RssFeedDto;
import com.kairos.trading.domain.settings.dto.UserSettingDto;
import com.kairos.trading.domain.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 설정 API.
 * 
 * 역할: 요청 검증 및 응답 변환만 담당.
 * 비즈니스 로직은 SettingsService에서 처리.
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 사용자 설정 조회.
     */
    @GetMapping
    public BaseResponse<UserSettingDto> getSettings() {
        log.debug("[API] 사용자 설정 조회");
        return BaseResponse.success(settingsService.getUserSettings());
    }

    /**
     * 투자 성향 변경.
     */
    @PutMapping("/strategy")
    public BaseResponse<UserSettingDto> updateStrategy(@RequestParam String mode) {
        log.info("[API] 투자 성향 변경: {}", mode);
        return BaseResponse.success(settingsService.updateStrategyMode(mode));
    }

    /**
     * RSS 피드 목록 조회.
     */
    @GetMapping("/rss-feeds")
    public BaseResponse<List<RssFeedDto>> getRssFeeds() {
        log.debug("[API] RSS 피드 목록 조회");
        return BaseResponse.success(settingsService.getRssFeeds());
    }

    /**
     * RSS 피드 추가.
     */
    @PostMapping("/rss-feeds")
    public BaseResponse<RssFeedDto> createRssFeed(@Valid @RequestBody CreateRssFeedRequest request) {
        log.info("[API] RSS 피드 추가: {} ({})", request.name(), request.category());
        return BaseResponse.success(settingsService.createRssFeed(request));
    }

    /**
     * RSS 피드 삭제.
     */
    @DeleteMapping("/rss-feeds/{id}")
    public BaseResponse<Void> deleteRssFeed(@PathVariable Long id) {
        log.info("[API] RSS 피드 삭제: {}", id);
        settingsService.deleteRssFeed(id);
        return BaseResponse.success(null);
    }
}
