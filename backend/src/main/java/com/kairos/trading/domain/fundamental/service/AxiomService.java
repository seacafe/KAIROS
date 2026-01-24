package com.kairos.trading.domain.fundamental.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.fundamental.agent.AxiomAiClient;
import com.kairos.trading.domain.fundamental.dto.FundamentalAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Axiom 재무 분석 서비스.
 * 
 * 역할:
 * 1. 재무제표 분석
 * 2. 한계 기업 필터링 (3년 연속 적자, 자본 잠식)
 * 3. 투자 부적격 판정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AxiomService {

    private final AxiomAiClient axiomAiClient;

    /**
     * 재무 데이터를 AI로 분석한다.
     */
    public FundamentalAnalysisDto analyzeFinancial(
            String stockCode,
            String stockName,
            String financialData) {
        log.debug("[Axiom] 재무 분석 시작: {} ({})", stockName, stockCode);

        var result = axiomAiClient.analyzeFinancial(stockCode, stockName, financialData);

        // 결과 로깅
        if (result.isRejected()) {
            log.warn("[Axiom] ❌ 투자 부적격: {} - {}", stockName, result.summary());
        } else if (result.isHighRisk()) {
            log.info("[Axiom] ⚠️ 고위험: {} - {}", stockName, result.summary());
        } else {
            log.debug("[Axiom] ✅ 재무 건전: {}", stockName);
        }

        return result;
    }

    /**
     * AgentResponse 형식으로 변환하여 반환한다.
     */
    public AgentResponse analyzeAndGetResponse(
            String stockCode,
            String stockName,
            String financialData) {
        var result = analyzeFinancial(stockCode, stockName, financialData);

        String decision = result.isRejected() ? "REJECT" : result.isHighRisk() ? "WATCH" : "BUY";

        int score = result.isRejected() ? 0 : result.isHighRisk() ? 40 : 80;

        return new AgentResponse(
                "Axiom",
                score,
                decision,
                result.summary(),
                Map.of(
                        "riskLevel", result.riskLevel(),
                        "debtRatio", result.debtRatio() != null ? result.debtRatio() : 0,
                        "consecutiveLoss", result.consecutiveLoss()));
    }
}
