package com.kairos.trading.domain.execution.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 수동 매도 요청 DTO.
 */
public record ManualSellRequest(
        @NotBlank(message = "종목코드는 필수입니다") String stockCode,

        @Min(value = 1, message = "수량은 1 이상이어야 합니다") int quantity,

        String reason) {
}
