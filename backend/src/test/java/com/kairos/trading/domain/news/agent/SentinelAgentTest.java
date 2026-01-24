package com.kairos.trading.domain.news.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sentinel 에이전트 테스트.
 * TDD Red Phase: 뉴스 분석 및 Kill Switch 로직 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Sentinel(NewsAgent) 테스트")
class SentinelAgentTest {

    @Mock
    private SentinelAiService aiService;

    private SentinelAgent sentinelAgent;

    @BeforeEach
    void setUp() {
        sentinelAgent = new SentinelAgent(aiService);
    }

    @Test
    @DisplayName("호재 뉴스 분석 시 높은 점수와 BUY 판정")
    void analyze_shouldReturnPositive_whenNewsIsGood() {
        // given
        var goodNews = "삼성전자, HBM3E 반도체 대량 수주 계약 체결. 내년 매출 30% 성장 전망.";
        var expectedResult = new NewsAnalysisResult(
                "005930", "삼성전자",
                List.of("HBM3E", "수주", "반도체"),
                "Positive", 85, "Low", false,
                "반도체 대형 수주 호재. 실적 개선 기대.");
        when(aiService.analyzeNews(anyString())).thenReturn(expectedResult);

        // when
        var result = sentinelAgent.analyze(goodNews);

        // then
        assertThat(result.isPositive()).isTrue();
        assertThat(result.materialStrength()).isGreaterThan(50);
        assertThat(result.requiresKillSwitch()).isFalse();
    }

    @Test
    @DisplayName("횡령 공시 감지 시 Kill Switch 발동")
    void analyze_shouldTriggerKillSwitch_whenEmbezzlementDetected() {
        // given
        var badNews = "[공시] ABC전자 대표이사 횡령 혐의로 검찰 조사 착수.";
        var expectedResult = new NewsAnalysisResult(
                "123456", "ABC전자",
                List.of("횡령", "검찰", "조사"),
                "Negative", -100, "High", true,
                "횡령 공시 감지. 즉시 Kill Switch 발동 필요.");
        when(aiService.analyzeNews(anyString())).thenReturn(expectedResult);

        // when
        var result = sentinelAgent.analyze(badNews);

        // then
        assertThat(result.requiresKillSwitch()).isTrue();
        assertThat(result.killSwitch()).isTrue();
        assertThat(result.materialStrength()).isEqualTo(-100);
    }

    @Test
    @DisplayName("거래정지 키워드 감지 시 Kill Switch 발동")
    void analyze_shouldTriggerKillSwitch_whenTradingHaltDetected() {
        // given
        var haltNews = "[긴급] XYZ코스닥 거래정지 결정. 상장폐지 심사 예정.";
        var expectedResult = new NewsAnalysisResult(
                "789012", "XYZ코스닥",
                List.of("거래정지", "상장폐지", "심사"),
                "Negative", -100, "High", true,
                "거래정지 감지. 즉시 매도 필요.");
        when(aiService.analyzeNews(anyString())).thenReturn(expectedResult);

        // when
        var result = sentinelAgent.analyze(haltNews);

        // then
        assertThat(result.requiresKillSwitch()).isTrue();
        assertThat(result.keywords()).containsAnyOf("거래정지", "상장폐지");
    }

    @Test
    @DisplayName("중립적 뉴스는 WATCH 판정")
    void analyze_shouldReturnNeutral_whenNewsIsNeutral() {
        // given
        var neutralNews = "삼성전자, 정기 주주총회 개최 예정.";
        var expectedResult = new NewsAnalysisResult(
                "005930", "삼성전자",
                List.of("주주총회", "정기"),
                "Neutral", 10, "Low", false,
                "일반적인 공시. 주가 영향 미미.");
        when(aiService.analyzeNews(anyString())).thenReturn(expectedResult);

        // when
        var result = sentinelAgent.analyze(neutralNews);

        // then
        assertThat(result.isPositive()).isFalse();
        assertThat(result.isNegative()).isFalse();
        assertThat(result.requiresKillSwitch()).isFalse();
    }

    @Test
    @DisplayName("Kill Switch 키워드가 텍스트에 포함되어 있으면 감지")
    void containsKillSwitchKeyword_shouldDetectDangerousText() {
        // given
        var dangerousText = "대표이사 배임 혐의로 기소";

        // when
        boolean contains = sentinelAgent.containsKillSwitchKeyword(dangerousText);

        // then
        assertThat(contains).isTrue();
    }

    @Test
    @DisplayName("안전한 텍스트에는 Kill Switch 키워드 없음")
    void containsKillSwitchKeyword_shouldReturnFalse_whenSafe() {
        // given
        var safeText = "삼성전자 신제품 출시 발표";

        // when
        boolean contains = sentinelAgent.containsKillSwitchKeyword(safeText);

        // then
        assertThat(contains).isFalse();
    }
}
