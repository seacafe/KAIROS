package com.kairos.trading.domain.flow.dto;

/**
 * 수급 분석 결과 DTO.
 * Sonar 에이전트가 반환하는 분석 결과.
 */
public record FlowAnalysisDto(
        String stockCode,
        String stockName,
        long foreignNet, // 외국인 순매수
        long institutionNet, // 기관 순매수
        long programNet, // 프로그램 순매수
        String flowType, // DoubleBuy, ForeignBuy, Distribution 등
        String priceDirection, // UP, DOWN, FLAT
        boolean isDistribution, // 설거지 패턴
        boolean isAccumulation, // 세력 모집 여부
        String decision, // BUY, REJECT, WATCH
        String summary) {
    /**
     * 양매수 패턴인지 확인
     */
    public boolean isDoubleBuy() {
        return "DoubleBuy".equals(flowType);
    }

    /**
     * 매도 우세인지 확인
     */
    public boolean isSelling() {
        return "Selling".equals(flowType);
    }
}
