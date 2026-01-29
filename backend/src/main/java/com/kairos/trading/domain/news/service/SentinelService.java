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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SentinelService {

    private final SentinelAiClient sentinelAiClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 뉴스를 분석하고 결과를 반환한다.
     */
    public NewsAnalysisDto analyzeNews(String newsText) {
        log.info("[Sentinel] 뉴스 분석 요청: {}", normalize(newsText));
        return sentinelAiClient.analyze(newsText);
    }

    /**
     * AgentResponse 형식으로 변환하여 반환한다.
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
                        "stockCode", result.stockCode() != null ? result.stockCode() : "",
                        "stockName", result.stockName() != null ? result.stockName() : "",
                        "keywords", result.keywords(),
                        "sentiment", result.sentiment(),
                        "killSwitch", result.killSwitch()));
    }

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
        // -100 ~ 100 -> 0 ~ 100
        return (materialStrength + 100) / 2;
    }

    public void publishKillSwitch(String stockCode, String stockName, String reason) {
        log.error("[Sentinel] ⚠️ KILL SWITCH 발행: {} ({}) - {}", stockName, stockCode, reason);
        eventPublisher.publishEvent(new KillSwitchEvent(
                this,
                stockCode,
                stockName,
                reason,
                "Sentinel"));
    }

    private String normalize(String text) {
        return text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}
