package com.kairos.trading.domain.strategy.controller;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.domain.strategy.dto.TargetStockDto;
import com.kairos.trading.domain.strategy.mapper.StrategyMapper;
import com.kairos.trading.domain.strategy.service.NexusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 추천 종목 API.
 * 
 * 역할: 요청 검증 및 응답 변환만 담당.
 * 비즈니스 로직은 NexusService에서 처리.
 */
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class TargetStockController {

    private final NexusService nexusService;
    private final StrategyMapper strategyMapper;

    /**
     * 당일 추천 종목 조회.
     */
    @GetMapping
    public BaseResponse<List<TargetStockDto>> getTargetStocks(
            @RequestParam(required = false) String date) {
        LocalDate baseDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        log.debug("[API] 추천 종목 조회: {}", baseDate);

        var targets = nexusService.getTodayTargets();
        return BaseResponse.success(strategyMapper.toTargetStockDtoList(targets));
    }

    /**
     * 매수 승인된 추천 종목만 조회.
     */
    @GetMapping("/buy")
    public BaseResponse<List<TargetStockDto>> getBuyTargets() {
        log.debug("[API] 매수 추천 종목 조회");

        var targets = nexusService.getBuyTargets();
        return BaseResponse.success(strategyMapper.toTargetStockDtoList(targets));
    }
}
