package com.kairos.trading.domain.strategy.service;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.strategy.agent.NexusAiClient;
import com.kairos.trading.domain.strategy.dto.StrategyDecisionDto;
import com.kairos.trading.domain.strategy.entity.TargetStock;
import com.kairos.trading.domain.strategy.repository.TargetStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NexusServiceTest {

        @InjectMocks
        private NexusService nexusService;

        @Mock
        private NexusAiClient nexusAiClient;

        @Mock
        private TargetStockRepository targetStockRepository;

        @Test
        @DisplayName("decide: Agent 중 하나라도 ALERT면 Kill Switch(ALERT) 반환")
        void decide_ShouldReturnAlert_WhenAnyAgentReportsAlert() {
                // Given
                List<AgentResponse> reports = List.of(
                                new AgentResponse("Sentinel", 80, "BUY", "Good", java.util.Map.of()),
                                new AgentResponse("Axiom", 0, "ALERT", "Risk Detected", java.util.Map.of()) // Kill
                                                                                                            // Switch
                                                                                                            // Trigger
                );

                // When
                StrategyDecisionDto result = nexusService.decide(reports, "Aggressive", "005930", "Samsung");

                // Then
                assertThat(result.decision()).isEqualTo("ALERT");
                assertThat(result.riskLevel()).isEqualTo("HIGH");
                assertThat(result.reasoning()).contains("Kill Switch 발동");

                // AI Client는 호출되지 않아야 함 (비용 절약 및 즉시 차단)
                verify(nexusAiClient, never()).decide(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("decide: ALERT가 없으면 AI Client를 호출하여 결정")
        void decide_ShouldCallAiClient_WhenNoAlert() {
                // Given
                List<AgentResponse> reports = List.of(
                                new AgentResponse("Sentinel", 80, "BUY", "Good", java.util.Map.of()),
                                new AgentResponse("Vector", 70, "WATCH", "Moving Average", java.util.Map.of()));
                StrategyDecisionDto expectedDecision = new StrategyDecisionDto(
                                "BUY", 85, "LOW", 0.5, 70000L, 68000L, "Strong Signal", null);

                given(nexusAiClient.decide(anyString(), anyString(), anyString(), anyString()))
                                .willReturn(expectedDecision);

                // When
                StrategyDecisionDto result = nexusService.decide(reports, "Aggressive", "005930", "Samsung");

                // Then
                assertThat(result).isEqualTo(expectedDecision);
                verify(nexusAiClient).decide(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("saveTargetStock: TargetStock 엔티티를 생성하고 저장해야 한다")
        void saveTargetStock_ShouldSaveEntity() {
                // Given
                StrategyDecisionDto decision = new StrategyDecisionDto(
                                "BUY", 85, "LOW", 0.5, 70000L, 68000L, "Reason", null);
                List<AgentResponse> reports = List.of(); // Empty for simplicity

                given(targetStockRepository.save(any(TargetStock.class)))
                                .willAnswer(invocation -> invocation.getArgument(0));

                // When
                TargetStock saved = nexusService.saveTargetStock("005930", "Samsung", decision, reports);

                // Then
                assertThat(saved.getStockCode()).isEqualTo("005930");
                assertThat(saved.getDecision()).isEqualTo("BUY");
                assertThat(saved.getNexusScore()).isEqualTo(85);
                assertThat(saved.getOriginalTargetPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
                assertThat(saved.getOriginalStopLoss()).isEqualByComparingTo(BigDecimal.valueOf(68000));

                verify(targetStockRepository).save(any(TargetStock.class));
        }

        @Test
        @DisplayName("getTodayTargets: Repository에서 당일 타겟 목록을 조회해야 한다")
        void getTodayTargets_ShouldReturnList() {
                // Given
                List<TargetStock> targets = List.of(TargetStock.builder().stockCode("005930").build());
                given(targetStockRepository.findByBaseDateOrderByNexusScoreDesc(any())).willReturn(targets);

                // When
                List<TargetStock> result = nexusService.getTodayTargets();

                // Then
                assertThat(result).hasSize(1);
                verify(targetStockRepository).findByBaseDateOrderByNexusScoreDesc(any());
        }

        @Test
        @DisplayName("getBuyTargets: Repository에서 당일 BUY 타겟 목록을 조회해야 한다")
        void getBuyTargets_ShouldReturnList() {
                // Given
                List<TargetStock> targets = List.of(TargetStock.builder().stockCode("000660").build());
                given(targetStockRepository.findBuyTargets(any())).willReturn(targets);

                // When
                List<TargetStock> result = nexusService.getBuyTargets();

                // Then
                assertThat(result).hasSize(1);
                verify(targetStockRepository).findBuyTargets(any());
        }
}
