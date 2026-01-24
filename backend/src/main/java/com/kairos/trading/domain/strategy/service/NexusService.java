package com.kairos.trading.domain.strategy.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.strategy.agent.NexusAiClient;
import com.kairos.trading.domain.strategy.dto.StrategyDecisionDto;
import com.kairos.trading.domain.strategy.entity.TargetStock;
import com.kairos.trading.domain.strategy.repository.TargetStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nexus 전략 의사결정 서비스.
 * 
 * 역할:
 * 1. 5인 분석가 리포트 종합
 * 2. 사용자 성향(Aggressive/Neutral/Stable) 반영
 * 3. 최종 의사결정 및 ExecutionOrder 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NexusService {

    private final NexusAiClient nexusAiClient;
    private final TargetStockRepository targetStockRepository;

    /**
     * 5인 분석가 리포트를 종합하여 최종 의사결정을 내린다.
     */
    public StrategyDecisionDto decide(
            List<AgentResponse> agentReports,
            String strategyMode,
            String stockCode,
            String stockName) {
        log.info("[Nexus] 전략 의사결정 시작: {} ({}) - 성향: {}",
                stockName, stockCode, strategyMode);

        // Kill Switch 사전 체크
        boolean hasKillSwitch = agentReports.stream()
                .anyMatch(r -> "ALERT".equals(r.decision()));

        if (hasKillSwitch) {
            log.error("[Nexus] ⚠️ KILL SWITCH 감지! 즉시 ALERT 반환");
            return new StrategyDecisionDto(
                    "ALERT", 0, "HIGH", 0.0, 0L, 0L,
                    "Kill Switch 발동으로 즉시 매도 필요", null);
        }

        // 리포트를 문자열로 변환
        String reportsJson = formatAgentReports(agentReports);

        // AI 의사결정
        var result = nexusAiClient.decide(strategyMode, stockCode, stockName, reportsJson);

        log.info("[Nexus] 의사결정 완료: {} (점수: {}, 리스크: {})",
                result.decision(), result.finalScore(), result.riskLevel());

        return result;
    }

    /**
     * 타겟 종목을 저장한다.
     */
    @Transactional
    public TargetStock saveTargetStock(
            String stockCode,
            String stockName,
            StrategyDecisionDto decision,
            List<AgentResponse> agentReports) {
        var target = TargetStock.builder()
                .baseDate(LocalDate.now())
                .stockCode(stockCode)
                .stockName(stockName)
                .decision(decision.decision())
                .riskLevel(decision.riskLevel())
                .nexusScore(decision.finalScore())
                .targetPrice(BigDecimal.valueOf(decision.targetPrice()))
                .stopLoss(BigDecimal.valueOf(decision.stopLossPrice()))
                .nexusReason(decision.reasoning())
                .status("WATCHING")
                .build();

        return targetStockRepository.save(target);
    }

    /**
     * 당일 타겟 종목 목록 조회.
     */
    public List<TargetStock> getTodayTargets() {
        return targetStockRepository.findByBaseDateOrderByNexusScoreDesc(LocalDate.now());
    }

    /**
     * 당일 BUY 판정 종목만 조회.
     */
    public List<TargetStock> getBuyTargets() {
        return targetStockRepository.findBuyTargets(LocalDate.now());
    }

    private String formatAgentReports(List<AgentResponse> reports) {
        StringBuilder sb = new StringBuilder();
        for (var report : reports) {
            sb.append(String.format(
                    "[%s] 점수: %d, 판정: %s, 사유: %s\n",
                    report.agentName(),
                    report.score(),
                    report.decision(),
                    report.reason()));
        }
        return sb.toString();
    }
}
