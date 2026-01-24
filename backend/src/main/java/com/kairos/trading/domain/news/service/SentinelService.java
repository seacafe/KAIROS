package com.kairos.trading.domain.news.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import com.kairos.trading.domain.news.agent.SentinelAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentinel 뉴스 분석 서비스.
 * 
 * 역할:
 * 1. 장전: Naver Search API로 주도 테마 발굴
 * 2. 장중: RSS 피드(DART/뉴스)로 공시 및 속보 감시
 * 3. 위기 감지: Kill Switch 키워드 발견 시 즉시 경고
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SentinelService {

    private final SentinelAiClient sentinelAiClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 뉴스/공시 텍스트를 AI로 분석한다.
     */
    public NewsAnalysisDto analyzeNews(String newsText) {
        log.debug("[Sentinel] 뉴스 분석 시작: {}...",
                newsText.substring(0, Math.min(50, newsText.length())));

        // 1. Kill Switch 키워드 사전 검사 (AI 호출 전 빠른 필터링)
        if (containsKillSwitchKeyword(newsText)) {
            log.warn("[Sentinel] Kill Switch 키워드 감지! 즉시 분석 수행.");
        }

        // 2. AI 분석 수행
        var result = sentinelAiClient.analyzeNews(newsText);

        // 3. 결과 로깅
        if (result.requiresKillSwitch()) {
            log.error("[Sentinel] ⚠️ KILL SWITCH 발동 필요: {} - {}",
                    result.stockName(), result.summary());
        } else if (result.isPositive()) {
            log.info("[Sentinel] 호재 감지: {} (강도: {})",
                    result.stockName(), result.materialStrength());
        }

        return result;
    }

    /**
     * AgentResponse 형식으로 변환하여 반환한다.
     * Nexus에게 전달할 표준 형식.
     */
    public AgentResponse analyzeAndGetResponse(String newsText) {
        var result = analyzeNews(newsText);

        String decision = determineDecision(result);
        int score = normalizeScore(result.materialStrength());

        return new AgentResponse(
                "Sentinel",
                score,
                decision,
                result.summary(),
                Map.of(
                        "stockCode", result.stockCode(),
                        "stockName", result.stockName(),
                        "keywords", result.keywords(),
                        "sentiment", result.sentiment(),
                        "killSwitch", result.killSwitch()));
    }

    /**
     * Kill Switch 키워드가 텍스트에 포함되어 있는지 확인한다.
     */
    public boolean containsKillSwitchKeyword(String text) {
        return NewsAnalysisDto.KILL_SWITCH_KEYWORDS.stream()
                .anyMatch(text::contains);
    }

    private String determineDecision(NewsAnalysisDto result) {
        if (result.requiresKillSwitch()) {
            return "ALERT";
        } else if (result.isPositive()) {
            return "BUY";
        } else if (result.isNegative()) {
            return "REJECT";
        } else {
            return "WATCH";
        }
    }

    private int normalizeScore(int materialStrength) {
        return (materialStrength + 100) / 2;
    }

    /**
     * Kill Switch 이벤트를 발행한다.
     */
    public void publishKillSwitch(String stockCode, String stockName, String reason) {
        log.error("[Sentinel] ⚠️ KILL SWITCH 발행: {} ({}) - {}", stockName, stockCode, reason);
        eventPublisher.publishEvent(new KillSwitchEvent(
                this,
                stockCode,
                stockName,
                reason,
                "Sentinel"));
    }
}
