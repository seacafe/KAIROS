package com.kairos.trading.domain.execution.controller;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.domain.execution.dto.ManualSellRequest;
import com.kairos.trading.domain.execution.dto.TradeLogDto;
import com.kairos.trading.domain.execution.service.TradeExecutionService;
import com.kairos.trading.domain.execution.service.TradeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 매매 로그 API.
 * 
 * 역할: 요청 검증 및 응답 변환만 담당.
 * 비즈니스 로직은 TradeLogService, TradeExecutionService에서 처리.
 */
@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeLogController {

    private final TradeLogService tradeLogService;
    private final TradeExecutionService executionService;

    /**
     * 당일 매매 로그 조회.
     */
    @GetMapping
    public BaseResponse<List<TradeLogDto>> getTradeLogs() {
        log.debug("[API] 당일 매매 로그 조회");
        return BaseResponse.success(tradeLogService.getTodayLogs());
    }

    /**
     * 수동 매도 요청.
     */
    @PostMapping("/manual-sell")
    public BaseResponse<Void> manualSell(@Valid @RequestBody ManualSellRequest request) {
        log.warn("[API] 수동 매도 요청: {} {}주 - {}",
                request.stockCode(), request.quantity(), request.reason());

        executionService.executeManualSell(request);
        return BaseResponse.success(null);
    }
}
