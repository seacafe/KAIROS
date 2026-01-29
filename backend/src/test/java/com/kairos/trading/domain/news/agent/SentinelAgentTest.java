package com.kairos.trading.domain.news.agent;

import com.kairos.trading.common.ai.AgentResponse;
import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Sentinel Agent 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Sentinel(NewsAgent) Unit Test")
class SentinelAgentTest {

    @Mock
    private SentinelAiClient sentinelAiClient;

    private SentinelAgent sentinelAgent;

    @BeforeEach
    void setUp() {
        sentinelAgent = new SentinelAgent(sentinelAiClient);
    }

    @Test
    @DisplayName("호재 뉴스 분석 시 BUY 판정 및 높은 점수")
    void analyze_shouldReturnBuy_whenNewsIsGood() {
        // given
        var goodNews = "삼성전자, HBM3E 반도체 대량 수주 계약 체결.";
        var analysisDto = new NewsAnalysisDto(
                "005930", "삼성전자",
                List.of("HBM3E", "수주"),
                "Positive", 80, "High", false,
                "반도체 대형 수주 호재.");

        given(sentinelAiClient.analyze(anyString())).willReturn(analysisDto);

        // when
        AgentResponse result = sentinelAgent.analyze("005930", "Samsung", goodNews);

        // then
        assertThat(result.decision()).isEqualTo("BUY");
        assertThat(result.score()).isGreaterThanOrEqualTo(90); // 50 + (80/2) = 90
        assertThat(result.agentName()).isEqualTo("Sentinel");
    }

    @Test
    @DisplayName("횡령 공시 감지 시 Kill Switch 및 ALERT 판정")
    void analyze_shouldReturnAlert_whenKillSwitchDetected() {
        // given
        var badNews = "대표이사 횡령 혐의 발생";
        var analysisDto = new NewsAnalysisDto(
                "123456", "BadCorp",
                List.of("횡령", "배임"),
                "Negative", -100, "High", true,
                "횡령 발생. 거래정지 우려.");

        given(sentinelAiClient.analyze(anyString())).willReturn(analysisDto);

        // when
        AgentResponse result = sentinelAgent.analyze("123456", "BadCorp", badNews);

        // then
        assertThat(result.decision()).isEqualTo("ALERT"); // Kill Switch -> ALERT
        assertThat(result.score()).isEqualTo(0);
    }

    @Test
    @DisplayName("중립적 뉴스는 WATCH 판정")
    void analyze_shouldReturnWatch_whenNewsIsNeutral() {
        // given
        var neutralNews = "주주총회 소집 공고";
        var analysisDto = new NewsAnalysisDto(
                "005930", "삼성전자",
                List.of("주총"),
                "Neutral", 10, "Low", false,
                "일반 공지.");

        given(sentinelAiClient.analyze(anyString())).willReturn(analysisDto);

        // when
        AgentResponse result = sentinelAgent.analyze("005930", "Samsung", neutralNews);

        // then
        assertThat(result.decision()).isEqualTo("WATCH");
        assertThat(result.score()).isBetween(50, 60); // 50 + 5 = 55
    }
}
