package com.kairos.trading.integration;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.common.event.AnalysisCompleteEvent;
import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.common.event.TickDataEvent;
import com.kairos.trading.domain.execution.service.TradeExecutionService;
import com.kairos.trading.domain.flow.agent.SonarAgent;
import com.kairos.trading.domain.sentiment.agent.ResonanceAgent;
import com.kairos.trading.domain.strategy.agent.NexusAgent;
import com.kairos.trading.domain.technical.agent.TechnicalAnalysisResult;
import com.kairos.trading.domain.technical.agent.VectorAgent;
import com.kairos.trading.domain.technical.service.NanoBananaCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarketSimulatorTest - 통합 시뮬레이션 테스트.
 * 
 * backendrule_kairos.md §1.5.3 준수
 * 실제 장 운영 환경을 모사하여 전체 플로우 검증.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MarketSimulator 통합 테스트")
class MarketSimulatorTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NexusAgent nexusAgent;

    @Autowired
    private VectorAgent vectorAgent;

    @Autowired
    private SonarAgent sonarAgent;

    @Autowired
    private ResonanceAgent resonanceAgent;

    @Autowired
    private NanoBananaCalculator calculator;

    @Autowired
    private TradeExecutionService executionService;

    @BeforeEach
    void setUp() {
        // MDC 설정 (테스트용)
        MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));
        MDC.put("threadId", String.valueOf(Thread.currentThread().threadId()));
    }

    @Test
    @DisplayName("[Golden Path] NanoBanana 패턴 감지 → 분석 → 의사결정 → 주문 플로우")
    void goldenPath_shouldExecuteBuyOrder_whenNanoBananaPatternDetected() throws InterruptedException {
        // === [09:01] Data Injection: NanoBanana 패턴 충족 데이터 ===
        // 이평선 수렴 (밀집)
        double ma5 = 72000;
        double ma20 = 71800;
        double ma60 = 71500;

        // 거래량 폭발 (3배)
        long todayVolume = 30_000_000;
        long avgVolume = 10_000_000;

        // === [09:01:05] Analysis: Vector 에이전트 분석 ===
        double convergence = calculator.calculateConvergence(ma5, ma20, ma60);
        double volumeRatio = calculator.calculateVolumeRatio(todayVolume, avgVolume);
        boolean isBullish = calculator.isBullishAlignment(ma5, ma20, ma60);

        assertThat(convergence).isGreaterThan(0.7);
        assertThat(volumeRatio).isGreaterThanOrEqualTo(2.0);
        assertThat(isBullish).isTrue();

        // NanoBanana 패턴 감지 확인
        boolean isNanoBanana = vectorAgent.detectNanoBananaPattern(ma5, ma20, ma60, todayVolume, avgVolume);
        assertThat(isNanoBanana).isTrue();

        // === 5인 에이전트 리포트 생성 ===
        var vectorResponse = createVectorResponse(true);
        var sonarResponse = sonarAgent.analyze("005930", "삼성전자", 50_000_000_000L, 30_000_000_000L, 10_000_000_000L);
        var resonanceResponse = resonanceAgent.analyze(65, 0.5, 1350.0);

        var reports = List.of(
                createSentinelResponse(true), // 뉴스 호재
                createAxiomResponse(true), // 재무 건전
                vectorResponse, // 차트 NanoBanana
                resonanceResponse, // 시장 심리 양호
                sonarResponse // 수급 양매수
        );

        // === [09:01:06] Decision: Nexus 전략가 의사결정 ===
        var decision = nexusAgent.decide(reports, "NEUTRAL");

        assertThat(decision.decision()).isEqualTo("BUY");
        assertThat(decision.score()).isGreaterThanOrEqualTo(60);

        // === [09:01:07] Execution: Aegis 주문 생성 ===
        var order = nexusAgent.createOrder(
                decision,
                "005930", "삼성전자", 10,
                BigDecimal.valueOf(72000),
                BigDecimal.valueOf(78000),
                BigDecimal.valueOf(69500));

        assertThat(order).isNotNull();
        assertThat(order.action()).isEqualTo("BUY");
        assertThat(order.stockCode()).isEqualTo("005930");

        // 주문 제출
        executionService.submitOrder(order);
        assertThat(executionService.getPendingOrderCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("[Kill Switch] 위기 감지 시 즉시 매도 플로우")
    void killSwitch_shouldTriggerImmediateSell_whenCrisisDetected() {
        // Sentinel이 횡령 공시 감지
        var alertReport = new AgentResponse(
                "Sentinel", 0, "ALERT",
                "횡령 공시 감지. 즉시 매도 필요.",
                Map.of("killSwitch", true, "stockCode", "123456"));

        var reports = List.of(
                alertReport,
                createAxiomResponse(true),
                createVectorResponse(true),
                resonanceAgent.analyze(70, 0.5, 1350.0),
                sonarAgent.analyze("123456", "ABC전자", 50_000_000_000L, 30_000_000_000L, 10_000_000_000L));

        // Nexus가 Kill Switch 감지
        var decision = nexusAgent.decide(reports, "NEUTRAL");

        assertThat(decision.decision()).isEqualTo("ALERT");
        assertThat(decision.metadata().get("killSwitch")).isEqualTo(true);
    }

    @Test
    @DisplayName("[Veto] 시장 공포 시 신규 진입 차단")
    void veto_shouldRejectEntry_whenMarketInFear() {
        // 시장 공포 상태 (점수 25)
        var resonanceVeto = resonanceAgent.analyze(25, -2.5, 1420.0);

        assertThat(resonanceVeto.decision()).isEqualTo("REJECT");

        // 진입 불허 확인
        boolean isAllowed = resonanceAgent.isEntryAllowed(25, "NEUTRAL");
        assertThat(isAllowed).isFalse();
    }

    @Test
    @DisplayName("[Strategy Mode] 성향별 의사결정 분기")
    void strategyMode_shouldAffectDecision_basedOnUserPreference() {
        var reports = List.of(
                createSentinelResponse(true),
                createAxiomResponse(true),
                createVectorResponse(true),
                resonanceAgent.analyze(55, 0.3, 1360.0), // 중간 점수
                sonarAgent.analyze("005930", "삼성전자", 20_000_000_000L, 10_000_000_000L, 5_000_000_000L));

        // AGGRESSIVE 모드: 점수 55로도 BUY 가능 (임계값 50)
        var aggressiveDecision = nexusAgent.decide(reports, "AGGRESSIVE");

        // STABLE 모드: 점수 55는 WATCH (임계값 70)
        var stableDecision = nexusAgent.decide(reports, "STABLE");

        // 같은 데이터지만 성향에 따라 결과가 다름
        assertThat(aggressiveDecision.decision()).isIn("BUY", "WATCH");
        assertThat(stableDecision.decision()).isIn("WATCH", "REJECT");
    }

    // === Helper Methods ===

    private AgentResponse createSentinelResponse(boolean isPositive) {
        if (isPositive) {
            return new AgentResponse("Sentinel", 85, "BUY", "호재 감지", Map.of());
        }
        return new AgentResponse("Sentinel", 30, "WATCH", "중립", Map.of());
    }

    private AgentResponse createAxiomResponse(boolean isHealthy) {
        if (isHealthy) {
            return new AgentResponse("Axiom", 75, "BUY", "재무 건전", Map.of());
        }
        return new AgentResponse("Axiom", 20, "REJECT", "재무 위험", Map.of());
    }

    private AgentResponse createVectorResponse(boolean isBullish) {
        if (isBullish) {
            return new AgentResponse("Vector", 90, "BUY", "NanoBanana 패턴",
                    Map.of("pattern", "NanoBanana", "entryPrice", 72000));
        }
        return new AgentResponse("Vector", 40, "WATCH", "횡보", Map.of());
    }
}
