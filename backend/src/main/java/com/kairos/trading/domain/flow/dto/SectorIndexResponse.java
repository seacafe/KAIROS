package com.kairos.trading.domain.flow.dto;

import java.util.List;

/**
 * 업종현재가요청(ka20001) 응답 DTO.
 * Sonar 에이전트가 업종 지수 및 섹터 흐름 파악에 활용.
 */
public record SectorIndexResponse(
        String sectorCode, // 업종코드 (upjong_cd)
        String sectorName, // 업종명 (upjong_nm)
        double currentIndex, // 현재지수 (cur_prc)
        double previousIndex, // 전일지수 (pred_jisu)
        double indexChange, // 전일대비 (flu_jisu)
        double changeRate, // 등락률 (flu_rt)
        long accVolume, // 누적거래량 (acc_trde_qty)
        long accAmount, // 누적거래대금 (acc_trde_amt)
        double openIndex, // 시가지수 (open_jisu)
        double highIndex, // 고가지수 (hgh_jisu)
        double lowIndex, // 저가지수 (low_jisu)
        List<SectorStock> topStocks // 상위종목 (upjong_stk_prst)
) {
    /**
     * 업종 구성 종목.
     */
    public record SectorStock(
            String stockCode, // 종목코드 (stk_cd)
            String stockName, // 종목명 (stk_nm)
            long currentPrice, // 현재가 (cur_prc)
            long priceChange, // 전일대비 (pred_pre)
            double changeRate, // 등락률 (flu_rt)
            long volume // 거래량 (trde_qty)
    ) {
    }
}
