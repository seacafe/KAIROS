package com.kairos.trading.integration;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.common.ai.MockAiClient;
import com.kairos.trading.domain.flow.agent.SonarAgent;
import com.kairos.trading.domain.fundamental.agent.AxiomAgent;
import com.kairos.trading.domain.news.agent.SentinelAgent;
import com.kairos.trading.domain.sentiment.agent.ResonanceAgent;
import com.kairos.trading.domain.strategy.agent.NexusAgent;
import com.kairos.trading.domain.strategy.agent.NexusAiClient;
import com.kairos.trading.domain.strategy.dto.StrategyDecisionDto;
import com.kairos.trading.domain.technical.agent.VectorAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * MarketSimulatorTest - 통합 시뮬레이션 테스트.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MarketSimulator 통합 테스트")
class MarketSimulatorTest {

        @Autowired
        private NexusAgent nexusAgent;

        @MockBean
        private SentinelAgent sentinelAgent;
        @MockBean
        private AxiomAgent axiomAgent;
        @MockBean
        private VectorAgent vectorAgent;
        @MockBean
        private SonarAgent sonarAgent;
        @MockBean
        private ResonanceAgent resonanceAgent;
        @MockBean
        private NexusAiClient nexusAiClient;

        @BeforeEach
        void setUp() {
                given(sentinelAgent.analyze(anyString(), anyString(), anyString()))
                                .willReturn(MockAiClient.mockSentinelResponse(true));

                given(axiomAgent.analyze(anyString(), anyString(), anyString()))
                                .willReturn(MockAiClient.mockAxiomResponse(true));

                given(vectorAgent.analyze(anyString(), anyString(), anyString(), anyString()))
                                .willReturn(MockAiClient.mockVectorResponse(true));

                given(sonarAgent.analyze(anyString(), anyString(), anyString(), anyString()))
                                .willReturn(MockAiClient.mockSonarResponse(true));

                given(resonanceAgent.analyze(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                                anyString()))
                                .willReturn(MockAiClient.mockResonanceResponse(80));

                // Nexus Mock
                AgentResponse mockNexusRes = MockAiClient.mockNexusResponse("AGGRESSIVE", 85);
                StrategyDecisionDto decisionDto = new StrategyDecisionDto(
                                mockNexusRes.decision(),
                                85,
                                "HIGH",
                                0.2, // positionSize
                                78000L,
                                69500L,
                                mockNexusRes.reason(),
                                null // dissent
                );

                given(nexusAiClient.decide(anyString(), anyString(), anyString(), anyString()))
                                .willReturn(decisionDto);
        }

        @Test
        @DisplayName("[Kill Switch] 횡령 뉴스 발생 시 즉시 경고")
        void whenEmbezzlementNews_thenTriggerKillSwitch() {
                given(sentinelAgent.analyze(anyString(), anyString(), anyString()))
                                .willReturn(MockAiClient.mockSentinelResponse(false));

                AgentResponse response = sentinelAgent.analyze("005930", "Samsung", "Embezzlement...");

                assertThat(response.decision()).isEqualTo("ALERT");
                assertThat(response.isAlert()).isTrue();
        }

        @Test
        @DisplayName("[Nexus] 5개 에이전트 만장일치 시 BUY 결정")
        void whenAllAgentsPositive_thenNexusDecidesBuy() {
                List<AgentResponse> reports = List.of(
                                sentinelAgent.analyze("code", "name", "news"),
                                axiomAgent.analyze("code", "name", "fin"),
                                vectorAgent.analyze("code", "name", "chart", "order"),
                                sonarAgent.analyze("code", "name", "trade", "prog"),
                                resonanceAgent.analyze(0, 0, 0, 0, 0, "news"));

                StrategyDecisionDto finalDecision = nexusAgent.decide("005930", "Samsung", reports, "AGGRESSIVE");

                assertThat(finalDecision.decision()).isEqualTo("BUY");
                assertThat(finalDecision.finalScore()).isGreaterThanOrEqualTo(80);
                assertThat(finalDecision.targetPrice()).isPositive();
        }
}
