package com.kairos.trading.domain.flow.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.flow.dto.FlowAnalysisDto;
import com.kairos.trading.domain.flow.agent.SonarAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sonar 수급 분석 서비스.
 * 
 * 역할:
 * 1. 실시간 프로그램 매매(0w) 추적
 * 2. 설거지 패턴(Fake Buying) 감지
 * 3. 외인/기관 양매수(Double Buy) 식별
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SonarService {

    private final SonarAiClient sonarAiClient;

    /**
     * 수급 데이터를 AI로 분석한다.
     */
    public FlowAnalysisDto analyzeFlow(
            String stockCode,
            String stockName,
            long foreignNet,
            long institutionNet,
            long programNet,
            double priceChange) {
        log.debug("[Sonar] 수급 분석 시작: {} ({})", stockName, stockCode);

        var result = sonarAiClient.analyzeFlow(
                stockCode, stockName,
                foreignNet, institutionNet, programNet, priceChange);

        // 결과 로깅
        if (result.isDoubleBuy()) {
            log.info("[Sonar] ✅ 양매수 감지: {} (외인: {}, 기관: {})",
                    stockName, foreignNet, institutionNet);
        }

        if (result.isDistribution()) {
            log.warn("[Sonar] ⚠️ 설거지 패턴: {} - {}", stockName, result.summary());
        }

        return result;
    }

    /**
     * 양매수 패턴 감지 (Java 로직).
     */
    public boolean isDoubleBuy(long foreignNet, long institutionNet) {
        return foreignNet > 0 && institutionNet > 0;
    }

    /**
     * 설거지 패턴 감지 (Java 로직).
     */
    public boolean isDistributionPattern(long programNet, double priceChange) {
        // 주가 상승 중 프로그램 대량 매도
        return priceChange > 0 && programNet < -10_000_000_000L; // -100억 이상 매도
    }

    /**
     * AgentResponse 형식으로 변환하여 반환한다.
     */
    public AgentResponse analyzeAndGetResponse(
            String stockCode,
            String stockName,
            long foreignNet,
            long institutionNet,
            long programNet,
            double priceChange) {
        var result = analyzeFlow(
                stockCode, stockName,
                foreignNet, institutionNet, programNet, priceChange);

        String decision = result.isDistribution() ? "REJECT" : result.isDoubleBuy() ? "BUY" : "WATCH";

        int score = result.isDistribution() ? 20 : result.isDoubleBuy() ? 90 : 50;

        return new AgentResponse(
                "Sonar",
                score,
                decision,
                result.summary(),
                Map.of(
                        "flowType", result.flowType(),
                        "foreignNet", foreignNet,
                        "institutionNet", institutionNet,
                        "isDistribution", result.isDistribution()));
    }
}
