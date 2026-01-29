package com.kairos.trading.domain.news.agent;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sentinel Agent (뉴스/공시 분석가).
 * 
 * 역할:
 * - 뉴스 타이틀 및 본문을 분석하여 호재/악재 판별
 * - Kill Switch 키워드 감지 시 즉시 경고
 * - SentinelAiClient(LLM)의 응답을 표준 AgentResponse로 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelAgent {

    private final SentinelAiClient sentinelAiClient;

    /**
     * 뉴스를 분석하여 의사결정을 내린다.
     * 
     * @param stockCode 종목코드
     * @param stockName 종목명
     * @param content   뉴스/공시 내용
     * @return 표준화된 분석 결과
     */
    public AgentResponse analyze(String stockCode, String stockName, String content) {
        log.info("[Sentinel] 분석 시작 - {} ({}): {}", stockName, stockCode, truncate(content, 50));

        try {
            // LLM 호출
            NewsAnalysisDto result = sentinelAiClient.analyze(content);

            // 점수 및 의사결정 매핑
            return mapToAgentResponse(result);

        } catch (Exception e) {
            log.error("[Sentinel] 분석 실패: {}", e.getMessage(), e);
            // 에러 시 중립 반환
            return new AgentResponse(
                    "Sentinel",
                    50,
                    "WATCH",
                    "분석 중 오류 발생: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    private AgentResponse mapToAgentResponse(NewsAnalysisDto dto) {
        // 1. Kill Switch 체크
        if (dto.requiresKillSwitch()) {
            return new AgentResponse(
                    "Sentinel",
                    0,
                    "ALERT",
                    "Kill Switch 발동: " + dto.summary(),
                    Map.of("dto", dto));
        }

        // 2. 점수 변환 (Material Strength -100~100 -> Score 0~100)
        // Strength > 0: 50 + (Strength / 2) -> 50~100
        // Strength < 0: 50 + (Strength / 2) -> 0~50
        int score = 50 + (dto.materialStrength() / 2);
        score = Math.max(0, Math.min(100, score));

        // 3. 의사결정 (Positive/High Urgency -> BUY, Negative -> REJECT, Else -> WATCH)
        String decision = "WATCH";
        if (dto.isPositive() && "High".equalsIgnoreCase(dto.urgency())) {
            decision = "BUY";
        } else if (dto.isNegative()) {
            decision = "REJECT";
        }

        return new AgentResponse(
                "Sentinel",
                score,
                decision,
                dto.stockName() + ": " + dto.summary(),
                Map.of("dto", dto));
    }

    private String truncate(String str, int len) {
        if (str == null)
            return "";
        return str.length() > len ? str.substring(0, len) + "..." : str;
    }
}
