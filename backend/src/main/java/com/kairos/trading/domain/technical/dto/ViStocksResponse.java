package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * VI 발동 종목 조회 응답 (ka10054).
 * Aegis 에이전트가 리스크 관리에 활용.
 */
public record ViStocksResponse(
        List<ViStock> stocks) {
    /**
     * VI 발동 종목.
     */
    public record ViStock(
            String stockCode,
            String stockName,
            String viType, // "1" 정적, "2" 동적
            String viStatus, // "발동", "해제"
            int triggerPrice,
            int referencePrice,
            String triggerTime) {
    }
}
