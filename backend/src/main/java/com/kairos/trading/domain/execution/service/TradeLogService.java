package com.kairos.trading.domain.execution.service;

import com.kairos.trading.domain.execution.dto.TradeLogDto;
import com.kairos.trading.domain.execution.mapper.ExecutionMapper;
import com.kairos.trading.domain.execution.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 매매 로그 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeLogService {

    private final TradeLogRepository tradeLogRepository;
    private final ExecutionMapper executionMapper;

    /**
     * 당일 매매 로그 조회.
     */
    public List<TradeLogDto> getTodayLogs() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        var logs = tradeLogRepository.findTodayLogs(startOfDay);
        return executionMapper.toTradeLogDtoList(logs);
    }

    /**
     * 슬리피지 과다 거래 조회.
     */
    public List<TradeLogDto> getHighSlippageTrades() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        var logs = tradeLogRepository.findHighSlippageTrades(startOfDay);
        return executionMapper.toTradeLogDtoList(logs);
    }
}
