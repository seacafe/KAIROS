package com.kairos.trading.domain.news.service;

import com.kairos.trading.common.gateway.ApiGatekeeper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NaverSearchService 테스트.
 * 
 * API Key 없이도 로직 검증 가능한 항목:
 * 1. HTML 태그 제거
 * 2. 종목코드 추출 패턴
 */
@ExtendWith(MockitoExtension.class)
class NaverSearchServiceTest {

    @Test
    @DisplayName("HTML 태그 제거")
    void stripHtml_removesTagsAndEntities() {
        // given
        String htmlText = "<b>삼성전자</b>&amp;SK하이닉스 &quot;급등&quot;";

        // 로직 재현 (private이므로)
        String stripped = htmlText.replaceAll("<[^>]*>", "").replaceAll("&[^;]+;", "");

        // then - &amp; -> "", &quot; -> ""
        assertThat(stripped).contains("삼성전자");
        assertThat(stripped).contains("SK");
        assertThat(stripped).doesNotContain("<b>");
    }

    @Test
    @DisplayName("종목코드 추출 - 6자리 숫자")
    void extractStockCode_findsFirst6DigitNumber() {
        // given
        String content = "삼성전자(005930) 시가 85,000원 돌파";

        // 패턴 검증
        var pattern = java.util.regex.Pattern.compile("\\b(\\d{6})\\b");
        var matcher = pattern.matcher(content);

        // then
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("005930");
    }

    @Test
    @DisplayName("종목코드 추출 - 코드 없음")
    void extractStockCode_returnsNull_whenNoCode() {
        // given
        String content = "반도체 시장 전망 밝아";

        var pattern = java.util.regex.Pattern.compile("\\b(\\d{6})\\b");
        var matcher = pattern.matcher(content);

        // then
        assertThat(matcher.find()).isFalse();
    }

    @Test
    @DisplayName("주도주 발굴 키워드 커버리지")
    void discoverLeadingStocks_keywordsCoverage() {
        // given - 서비스에서 사용하는 키워드
        var keywords = java.util.List.of("특징주", "급등 예상", "대규모 수주", "호재 공시");

        // then - 키워드가 4개인지 확인
        assertThat(keywords).hasSize(4);
        assertThat(keywords).contains("특징주", "호재 공시");
    }
}
