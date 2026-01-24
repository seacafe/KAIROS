package com.kairos.trading.domain.fundamental.dto;

/**
 * 재무 분석 결과 DTO.
 * Axiom 에이전트가 반환하는 분석 결과.
 */
public record FundamentalAnalysisDto(
        String stockCode,
        String stockName,
        Double per, // Price/Earnings Ratio
        Double pbr, // Price/Book Ratio
        Double roe, // Return on Equity
        Double debtRatio, // 부채비율
        boolean operatingProfit, // 영업이익 흑자 여부
        int consecutiveLoss, // 연속 적자 연수
        boolean capitalErosion, // 자본 잠식 여부
        String decision, // PASS, REJECT
        String riskLevel, // HIGH, MEDIUM, LOW
        String summary // 분석 요약
) {
    /**
     * 투자 부적격 종목인지 확인 (상장폐지 위험)
     */
    public boolean isRejected() {
        return "REJECT".equals(decision);
    }

    /**
     * 고위험인지 확인
     */
    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel);
    }

    /**
     * 우량주인지 확인
     */
    public boolean isBlueChip() {
        return operatingProfit &&
                debtRatio != null && debtRatio < 100 &&
                roe != null && roe > 10 &&
                "LOW".equals(riskLevel);
    }

    /**
     * 저평가 종목인지 확인
     */
    public boolean isUndervalued() {
        return per != null && per > 0 && per < 10 &&
                pbr != null && pbr < 1.0;
    }
}
