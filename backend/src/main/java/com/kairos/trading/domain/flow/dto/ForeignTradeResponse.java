package com.kairos.trading.domain.flow.dto;

import java.util.List;

/**
 * 외국인 종목별 매매동향 응답 (ka10008).
 * Sonar 에이전트가 외국인 수급 분석에 활용.
 */
public record ForeignTradeResponse(
        String stockCode,
        String stockName,
        List<ForeignTradeDay> dailyData) {
    /**
     * 일별 외국인 매매 데이터.
     */
    public record ForeignTradeDay(
            String date,
            int closePrice,
            int priceChange,
            double changeRate,
            long volume,
            long foreignNetBuy, // 외국인 순매수 (주)
            long foreignHolding, // 외국인 보유주수
            double foreignRatio // 외국인 보유비율 (%)
    ) {
    }

    /**
     * 최근 N일 외국인 순매수 합계.
     */
    public long getRecentNetBuy(int days) {
        return dailyData.stream()
                .limit(days)
                .mapToLong(ForeignTradeDay::foreignNetBuy)
                .sum();
    }

    /**
     * 외국인 연속 순매수 일수.
     */
    public int getConsecutiveBuyDays() {
        int count = 0;
        for (ForeignTradeDay day : dailyData) {
            if (day.foreignNetBuy() > 0)
                count++;
            else
                break;
        }
        return count;
    }
}
