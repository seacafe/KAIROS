package com.kairos.trading.domain.flow.dto;

import java.util.List;

/**
 * 프로그램 매매 응답 DTO (ka90003, ka90004).
 * 
 * Sonar 에이전트가 수급 분석에 활용.
 */
public record ProgramTradeResponse(
        // 종목 리스트
        List<ProgramTradeStock> stocks) {
    /**
     * 종목별 프로그램 매매 DTO.
     */
    public record ProgramTradeStock(
            int rank, // 순위
            String stockCode, // 종목코드
            String stockName, // 종목명
            int currentPrice, // 현재가
            String fluctuationSign, // 등락기호
            int changeFromPrev, // 전일대비
            double changeRate, // 등락율
            long accumulatedVolume, // 누적거래량
            long programSellAmount, // 프로그램매도금액
            long programBuyAmount, // 프로그램매수금액
            long programNetBuyAmount // 프로그램순매수금액
    ) {
        /**
         * 프로그램 순매수 여부.
         */
        public boolean isNetBuying() {
            return programNetBuyAmount > 0;
        }

        /**
         * 강한 순매수 여부 (10억 이상).
         */
        public boolean isStrongBuying() {
            return programNetBuyAmount >= 10_0000_0000L;
        }

        /**
         * 강한 순매도 여부 (-10억 이상).
         */
        public boolean isStrongSelling() {
            return programNetBuyAmount <= -10_0000_0000L;
        }
    }

    /**
     * 순매수 상위 종목 필터링.
     */
    public List<ProgramTradeStock> getNetBuyingStocks() {
        return stocks.stream()
                .filter(ProgramTradeStock::isNetBuying)
                .toList();
    }

    /**
     * 강한 순매수 종목만 필터링.
     */
    public List<ProgramTradeStock> getStrongBuyingStocks() {
        return stocks.stream()
                .filter(ProgramTradeStock::isStrongBuying)
                .toList();
    }

    /**
     * 총 프로그램 순매수 금액.
     */
    public long totalNetBuyAmount() {
        return stocks.stream()
                .mapToLong(ProgramTradeStock::programNetBuyAmount)
                .sum();
    }
}
