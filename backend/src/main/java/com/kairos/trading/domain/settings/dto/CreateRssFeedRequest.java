package com.kairos.trading.domain.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * RSS 피드 생성 요청 DTO.
 */
public record CreateRssFeedRequest(
        @NotBlank(message = "피드 이름은 필수입니다") String name,

        @NotBlank(message = "피드 URL은 필수입니다") @Pattern(regexp = "^https?://.*", message = "유효한 URL 형식이어야 합니다") String url,

        @NotBlank(message = "카테고리는 필수입니다") @Pattern(regexp = "DOMESTIC|DISCLOSURE|GLOBAL", message = "DOMESTIC, DISCLOSURE, GLOBAL 중 하나여야 합니다") String category) {
}
