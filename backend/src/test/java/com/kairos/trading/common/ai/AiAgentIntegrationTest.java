package com.kairos.trading.common.ai;

import com.kairos.trading.domain.news.agent.SentinelAgent;
import com.kairos.trading.domain.news.agent.SentinelAiClient;
import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import com.kairos.trading.domain.strategy.agent.NexusAgent;
import com.kairos.trading.domain.strategy.agent.NexusAiClient;
import com.kairos.trading.domain.strategy.dto.StrategyDecisionDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AI 에이전트 통합 테스트.
 * 
 * LLM 호출을 Mock하여 에이전트 로직 검증.
 * 실제 Gemini API 호출은 E2E 테스트에서 별도 수행.
 */
@ExtendWith(MockitoExtension.class)
class AiAgentIntegrationTest {

    @Mock
    private SentinelAiClient sentinelAiClient;

    @Mock
    private NexusAiClient nexusAiClient;

    @Nested
    @DisplayName("Sentinel Agent 테스트")
    class SentinelAgentTest {

        private SentinelAgent sentinelAgent;

        @BeforeEach
        void setup() {
            sentinelAgent = new SentinelAgent(sentinelAiClient);
        }

        @Test
        @DisplayName("호재 뉴스 분석 - BUY 의사결정")
        void positiveNews_returnsBuyDecision() {
            // given
            var analysisDto = new NewsAnalysisDto(
                    "005930",
                    "삼성전자",
                    List.of("수주", "호재"),
                    "Positive",
                    80, // materialStrength
                    "High",
                    false,
                    "삼성전자 대규모 수주 발표");

            when(sentinelAiClient.analyze(anyString())).thenReturn(analysisDto);

            // when
            var result = sentinelAgent.analyze("005930", "삼성전자", "삼성전자 대규모 수주 발표");

            // then
            assertThat(result.agentName()).isEqualTo("Sentinel");
            assertThat(result.decision()).isEqualTo("BUY");
            assertThat(result.score()).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("악재 뉴스 분석 - REJECT 의사결정")
        void negativeNews_returnsRejectDecision() {
            // given
            var analysisDto = new NewsAnalysisDto(
                    "000660",
                    "SK하이닉스",
                    List.of("하락", "악재"),
                    "Negative",
                    -60,
                    "Low",
                    false,
                    "SK하이닉스 실적 부진 예상");

            when(sentinelAiClient.analyze(anyString())).thenReturn(analysisDto);

            // when
            var result = sentinelAgent.analyze("000660", "SK하이닉스", "실적 부진 예상");

            // then
            assertThat(result.decision()).isEqualTo("REJECT");
            assertThat(result.score()).isLessThan(50);
        }

        @Test
        @DisplayName("Kill Switch 뉴스 - ALERT 발생")
        void killSwitchNews_returnsAlert() {
            // given
            var analysisDto = new NewsAnalysisDto(
                    "999999",
                    "XYZ종목",
                    List.of("상장폐지"),
                    "Negative",
                    -100,
                    "High",
                    true, // killSwitch
                    "상장폐지 결정");

            when(sentinelAiClient.analyze(anyString())).thenReturn(analysisDto);

            // when
            var result = sentinelAgent.analyze("999999", "XYZ종목", "상장폐지 결정");

            // then
            assertThat(result.decision()).isEqualTo("ALERT");
            assertThat(result.score()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Nexus Agent 테스트")
    class NexusAgentTest {

        private NexusAgent nexusAgent;

        @BeforeEach
        void setup() {
            nexusAgent = new NexusAgent(nexusAiClient);
        }

        @Test
        @DisplayName("전략 결정 - BUY")
        void strategicDecision_buy() {
            // given
            var decisionDto = new StrategyDecisionDto(
                    "BUY",
                    85,
                    "MEDIUM",
                    0.15,
                    90000,
                    82000,
                    "5명 에이전트 중 4명이 매수 추천",
                    null);

            when(nexusAiClient.decide(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(decisionDto);

            var reports = List.of(
                    new AgentResponse("Sentinel", 85, "BUY", "호재 감지", null),
                    new AgentResponse("Vector", 80, "BUY", "상승 패턴", null),
                    new AgentResponse("Sonar", 75, "BUY", "수급 양호", null));

            // when
            var result = nexusAgent.decide("005930", "삼성전자", reports, "Aggressive");

            // then
            assertThat(result.decision()).isEqualTo("BUY");
            assertThat(result.finalScore()).isEqualTo(85);
            assertThat(result.positionSize()).isEqualTo(0.15);
        }

        @Test
        @DisplayName("에러 발생 시 보수적 WATCH 반환")
        void onError_returnsConservativeWatchDecision() {
            // given
            when(nexusAiClient.decide(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("LLM 호출 실패"));

            var reports = List.of(
                    new AgentResponse("Sentinel", 50, "WATCH", "중립", null));

            // when
            var result = nexusAgent.decide("005930", "삼성전자", reports, "Stable");

            // then
            assertThat(result.decision()).isEqualTo("WATCH");
            assertThat(result.riskLevel()).isEqualTo("HIGH");
            assertThat(result.positionSize()).isEqualTo(0.0);
        }
    }
}
