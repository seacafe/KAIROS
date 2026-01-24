package com.kairos.trading.domain.strategy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 추천 종목 응답 DTO.
 */
public record TargetStockDto(
        Long id,
        LocalDate baseDate,
        String stockCode,
        String stockName,
        String decision, // BUY, WATCH, REJECT
        String riskLevel, // HIGH, MEDIUM, LOW
        int nexusScore, // 종합 점수
        BigDecimal targetPrice, // 목표가
        BigDecimal stopLoss, // 손절가
        String status, // WATCHING, TRADED, ENDED
        Map<String, Integer> agentScores, // 에이전트별 점수
        String nexusReason // AI 선정 사유
) {
}
