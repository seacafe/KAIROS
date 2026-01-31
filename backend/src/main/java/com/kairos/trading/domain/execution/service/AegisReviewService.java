package com.kairos.trading.domain.execution.service;

import com.kairos.trading.domain.execution.agent.AegisReviewAiClient;
import com.kairos.trading.domain.execution.dto.SlippageAnalysisDto;
import com.kairos.trading.domain.execution.entity.TradeLog;
import com.kairos.trading.domain.execution.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aegis 장후 리뷰 서비스.
 * 
 * 역할:
 * 1. 슬리피지 분석
 * 2. 주문 타이밍 보정값 제안
 * 
 * 주의: 장중 실행은 TradeExecutionService (100% Java)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AegisReviewService {

    private final AegisReviewAiClient aegisReviewAiClient;
    private final TradeLogRepository tradeLogRepository;

    /**
     * 당일 슬리피지 과다 거래를 분석한다.
     */
    public List<SlippageAnalysisDto> analyzeSlippage(String orderBookHistory) {
        log.info("[Aegis Review] 슬리피지 분석 시작");

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<TradeLog> highSlippageTrades = tradeLogRepository.findHighSlippageTrades(startOfDay);

        if (highSlippageTrades.isEmpty()) {
            log.info("[Aegis Review] 슬리피지 과다 거래 없음");
            return List.of();
        }

        log.info("[Aegis Review] 분석 대상: {}건", highSlippageTrades.size());

        // 거래 로그를 문자열로 변환
        String tradeLogs = formatTradeLogs(highSlippageTrades);

        // AI 분석
        var result = aegisReviewAiClient.analyzeSlippage(tradeLogs, orderBookHistory);

        log.info("[Aegis Review] 분석 완료 - 원인: {}, 보정값: {}ms",
                result.cause(), result.timeOffsetMs());

        return List.of(result);
    }

    /**
     * 당일 거래 통계 조회.
     */
    public TradeStats getTodayStats() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<TradeLog> todayLogs = tradeLogRepository.findTodayLogs(startOfDay);

        long totalTrades = todayLogs.size();
        long wins = todayLogs.stream()
                .filter(t -> "SELL".equals(t.getTradeType()) &&
                        t.getProfitLoss() != null &&
                        t.getProfitLoss().doubleValue() > 0)
                .count();

        double avgSlippage = todayLogs.stream()
                .filter(t -> t.getSlippageRate() != null)
                .mapToDouble(t -> t.getSlippageRate().doubleValue())
                .average()
                .orElse(0.0);

        return new TradeStats(totalTrades, wins, avgSlippage);
    }

    private String formatTradeLogs(List<TradeLog> logs) {
        StringBuilder sb = new StringBuilder();
        for (var log : logs) {
            sb.append(String.format(
                    "ID: %d, 종목: %s, 주문가: %s, 체결가: %s, 슬리피지: %.2f%%\n",
                    log.getId(),
                    log.getStockName(),
                    log.getOrderPrice(),
                    log.getFilledPrice(),
                    log.getSlippageRate()));
        }
        return sb.toString();
    }

    public record TradeStats(long totalTrades, long wins, double avgSlippage) {
        public double winRate() {
            return totalTrades > 0 ? (double) wins / totalTrades * 100 : 0;
        }
    }
}
