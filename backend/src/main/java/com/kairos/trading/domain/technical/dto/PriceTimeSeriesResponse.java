package com.kairos.trading.domain.technical.dto;

import java.util.List;

/**
 * 주식일주월시분요청(ka10005) 응답 DTO.
 * Vector 에이전트가 다양한 시간대(일/주/월/시/분) 시세 분석에 활용.
 */
public record PriceTimeSeriesResponse(
        String stockCode, // 종목코드 (stk_cd)
        String stockName, // 종목명 (stk_nm)
        long currentPrice, // 현재가 (cur_prc)
        long priceChange, // 전일대비 (flu_prc)
        double changeRate, // 등락률 (flu_rt)
        long accVolume, // 누적거래량 (acc_trde_qty)
        List<TimeSeriesData> timeSeries // 시계열 데이터 (stk_dt_pole)
) {
    /**
     * 시계열 데이터 항목.
     */
    public record TimeSeriesData(
            String date, // 일자 (dt)
            long openPrice, // 시가 (open_prc)
            long highPrice, // 고가 (hgh_prc)
            long lowPrice, // 저가 (low_prc)
            long closePrice, // 종가 (clos_prc)
            long volume, // 거래량 (trde_qty)
            long amount, // 거래대금 (trde_amt)
            long adjustedClose // 수정종가 (adj_clos_prc)
    ) {
    }
}
