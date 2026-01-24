package com.kairos.trading.common.event;

import com.kairos.trading.domain.execution.service.TradeExecutionService;
import com.kairos.trading.domain.flow.service.SonarService;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import com.kairos.trading.domain.strategy.service.NexusService;
import com.kairos.trading.domain.technical.service.NanoBananaCalculator;
import com.kairos.trading.domain.technical.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 트레이딩 이벤트 리스너.
 * 
 * WebSocket에서 수신된 이벤트를 각 에이전트에 전달하고,
 * 분석 결과에 따라 후속 이벤트를 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingEventListener {

    private final NanoBananaCalculator nanoBananaCalculator;
    private final VectorService vectorService;
    private final SonarService sonarService;
    private final NexusService nexusService;
    private final TradeExecutionService executionService;
    private final ApplicationEventPublisher eventPublisher;

    // 종목별 이평선 캐시 (실시간 업데이트)
    private final Map<String, MovingAverageCache> maCache = new ConcurrentHashMap<>();

    /**
     * 체결가 이벤트 처리.
     * NanoBanana 패턴 감지 시 분석 완료 이벤트 발행.
     */
    @Async
    @EventListener
    public void onTickData(TickDataEvent event) {
        String stockCode = event.getStockCode();
        log.trace("[EventListener] 체결: {} @ {} ({}%)",
                stockCode, event.getPrice(), event.getChangeRate());

        // 캐시에서 이평선 조회 (없으면 스킵)
        var cache = maCache.get(stockCode);
        if (cache == null) {
            return;
        }

        // NanoBanana 패턴 체크 (Pure Java, 실시간)
        boolean isPattern = vectorService.detectNanoBananaPattern(
                cache.ma5, cache.ma20, cache.ma60,
                event.getVolume(), cache.avgVolume);

        if (isPattern) {
            log.info("[EventListener] 🍌 NanoBanana 감지: {} @ {}", stockCode, event.getPrice());

            // 분석 완료 이벤트 발행 → Nexus로 전달
            eventPublisher.publishEvent(new AnalysisCompleteEvent(
                    this,
                    stockCode,
                    cache.stockName,
                    "NANO_BANANA",
                    85, // 패턴 점수
                    event.getPrice(),
                    "NanoBanana 패턴 감지 - 이평선 수렴 후 거래량 폭발"));
        }
    }

    /**
     * 프로그램 매매 이벤트 처리.
     * 설거지 패턴(대량 프로그램 매도) 감지 시 경고.
     */
    @Async
    @EventListener
    public void onProgramTrade(ProgramTradeEvent event) {
        String stockCode = event.getStockCode();

        // 설거지 패턴 체크 (주가 상승 중 프로그램 순매도)
        if (event.isDistributionPattern()) {
            log.warn("[EventListener] ⚠️ 설거지 패턴 감지: {} (프로그램 순매도: {}억)",
                    stockCode, event.getProgramNet() / 100_000_000);

            // Sonar 에이전트 알림 (추가 분석 트리거 가능)
        }
    }

    /**
     * VI 발동 이벤트 처리.
     * 보유 종목 VI 발동 시 Kill Switch 검토.
     */
    @Async
    @EventListener
    public void onViEvent(ViEvent event) {
        log.warn("[EventListener] 🚨 VI 발동: {} ({}) @ {}",
                event.getStockName(), event.getViType(), event.getTriggerPrice());

        // 하락 VI인 경우 Kill Switch 발동 검토
        if ("DOWN".equals(event.getViType())) {
            eventPublisher.publishEvent(new KillSwitchEvent(
                    this,
                    event.getStockCode(),
                    event.getStockName(),
                    "하락 VI 발동",
                    "ViEvent"));
        }
    }

    /**
     * 분석 완료 이벤트 처리.
     * Nexus에게 전달하여 최종 의사결정 요청.
     */
    @Async
    @EventListener
    public void onAnalysisComplete(AnalysisCompleteEvent event) {
        log.info("[EventListener] 분석 완료: {} - {} (점수: {})",
                event.getStockCode(), event.getAnalysisType(), event.getScore());

        // 점수가 70 이상이면 Nexus에게 의사결정 요청
        if (event.getScore() >= 70) {
            // TODO: 5인 분석가 리포트 수집 후 Nexus.decide() 호출
            log.info("[EventListener] 고점수 종목 → Nexus 의사결정 요청: {}", event.getStockCode());
        }
    }

    /**
     * 종목 이평선 캐시 업데이트.
     * 장 시작 시 또는 주기적으로 호출.
     */
    public void updateMovingAverageCache(String stockCode, String stockName,
            double ma5, double ma20, double ma60,
            long avgVolume) {
        maCache.put(stockCode, new MovingAverageCache(stockName, ma5, ma20, ma60, avgVolume));
        log.debug("[EventListener] MA 캐시 업데이트: {} (MA5={}, MA20={}, MA60={})",
                stockCode, ma5, ma20, ma60);
    }

    /**
     * 이동평균 캐시 레코드.
     */
    private record MovingAverageCache(
            String stockName,
            double ma5,
            double ma20,
            double ma60,
            long avgVolume) {
    }
}
