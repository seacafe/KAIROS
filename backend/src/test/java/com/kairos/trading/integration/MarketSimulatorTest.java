package com.kairos.trading.integration;

import com.kairos.trading.common.ai.AgentResponse;

import com.kairos.trading.common.event.TickDataEvent;
import com.kairos.trading.common.event.TradingEventListener;
import com.kairos.trading.domain.execution.service.TradeExecutionService;
import com.kairos.trading.domain.flow.agent.SonarAgent;
import com.kairos.trading.domain.fundamental.agent.AxiomAgent;
import com.kairos.trading.domain.news.agent.SentinelAgent;
import com.kairos.trading.domain.sentiment.agent.ResonanceAgent;
import com.kairos.trading.domain.strategy.agent.NexusAiClient;
import com.kairos.trading.domain.strategy.dto.StrategyDecisionDto;
import com.kairos.trading.domain.strategy.service.NexusService;
import com.kairos.trading.domain.technical.agent.VectorAiClient;
import com.kairos.trading.domain.technical.dto.TechnicalAnalysisDto;
import com.kairos.trading.domain.technical.service.VectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verify;

/**
 * MarketSimulatorTest - 통합 시스템 시뮬레이션.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MarketSimulator 통합 테스트 (Full Logic)")
class MarketSimulatorTest {

        @Autowired
        private ApplicationEventPublisher eventPublisher;

        @MockitoSpyBean
        private TradingEventListener tradingEventListener;

        @MockitoSpyBean
        private VectorService vectorService; // Spy to verify internal calls

        @MockitoSpyBean
        private NexusService nexusService; // Spy execution

        @MockitoSpyBean
        private TradeExecutionService executionService; // Spy execution

        // -- Mock Agents (simulating AI Logic) --
        @MockitoBean
        private SentinelAgent sentinelAgent;
        @MockitoBean
        private AxiomAgent axiomAgent;
        @MockitoBean
        private SonarAgent sonarAgent;
        @MockitoBean
        private ResonanceAgent resonanceAgent;

        // -- Mock AI Clients for Real Services --
        @MockitoBean
        private VectorAiClient vectorAiClient;
        @MockitoBean
        private NexusAiClient nexusAiClient;

        @BeforeEach
        void setUp() {
                // 1. Setup Mock Agents to return PASSED responses
                given(sentinelAgent.analyze(anyString(), anyString(), anyString()))
                                .willReturn(new AgentResponse("Sentinel", 90, "BUY", "Good News", Map.of()));

                given(axiomAgent.analyze(anyString(), anyString(), anyString()))
                                .willReturn(new AgentResponse("Axiom", 80, "BUY", "Solid Financials", Map.of()));

                given(sonarAgent.analyze(anyString(), anyString(), anyString(), anyString()))
                                .willReturn(new AgentResponse("Sonar", 85, "BUY", "Foreign Buying", Map.of()));

                given(resonanceAgent.analyze(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                                anyString()))
                                .willReturn(new AgentResponse("Resonance", 80, "BUY", "Risk On", Map.of()));

                // 2. Setup VectorAiClient (used by VectorService)
                // TechnicalAnalysisDto Constructor: (code, name, pattern, valid, entry, target,
                // sl, conv, volRatio, obRatio, fake, score, summary)
                TechnicalAnalysisDto vectorResult = new TechnicalAnalysisDto(
                                "005930", "Samsung", // code, name
                                "NanoBanana", true, // pattern, valid
                                50000, 52000, 49000, // entry, target, sl
                                0.8, 2.5, 1.2, false, // conv, volRatio, obRatio, fake
                                85, "Perfect Banana" // score, summary
                );
                given(vectorAiClient.analyzeChart(anyString(), anyString(), anyLong(), anyDouble(), anyDouble(),
                                anyDouble(), anyLong(), anyLong(), anyString()))
                                .willReturn(vectorResult);

                // 3. Setup NexusAiClient (used by NexusService)
                StrategyDecisionDto nexusDecision = new StrategyDecisionDto(
                                "BUY", 90, "HIGH", 0.1, 52000, 49000, "Unanimous Buy", null);
                // decide(strategyMode, stockCode, stockName, agentReports)
                given(nexusAiClient.decide(anyString(), anyString(), anyString(), anyString()))
                                .willReturn(nexusDecision);
        }

        @Test
        @DisplayName("[Scenario A] Happy Path: 급등 데이터 주입 -> 주문 실행")
        void scenario_HappyPath() {
                String stockCode = "005930";
                String stockName = "Samsung";

                // 1. Pre-condition: Prepare MA Cache for Pattern
                // Condition: Convergence >= 0.7, VolumeRatio >= 2.0, Bullish
                tradingEventListener.updateMovingAverageCache(stockCode, stockName, 10100, 10050, 10000, 100000);

                // 2. Action: Inject Tick Data (Volume Explosion -> 300,000)
                // Constructor: (source, stockCode, price, volume, accVolume, changeRate)
                TickDataEvent tickEvent = new TickDataEvent(
                                this, stockCode,
                                10200, // price
                                1000, // volume (current tick)
                                300000, // accVolume (3x avg)
                                2.0 // change rate
                );

                eventPublisher.publishEvent(tickEvent);

                // 3. Verification: Wait for Async processing
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        // Debug: Check if VectorService was called (Logic Trigger)
                        verify(vectorService).detectNanoBananaPattern(
                                        anyDouble(), anyDouble(), anyDouble(), anyLong(), anyLong());

                        // A. Check if NexusService was called
                        verify(nexusService).decide(any(), any(), eq(stockCode), eq(stockName));

                        // B. Check if Order was submitted to Aegis
                        verify(executionService).submitOrder(argThat(order -> "BUY".equals(order.action())));
                });

                // 4. Final Confirmation
                verify(executionService).processNextOrder();
        }
}
