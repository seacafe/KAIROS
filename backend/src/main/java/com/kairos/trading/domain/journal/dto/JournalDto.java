package com.kairos.trading.domain.journal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매매일지 응답 DTO.
 */
public record JournalDto(
        Long id,
        LocalDate date,
        BigDecimal totalProfitLoss,
        BigDecimal winRate,
        int tradeCount,
        String aiReviewContent,
        String improvementPoints) {
}
