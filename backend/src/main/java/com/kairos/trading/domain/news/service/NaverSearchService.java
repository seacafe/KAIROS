package com.kairos.trading.domain.news.service;

import com.kairos.trading.common.gateway.ApiGatekeeper;
import com.kairos.trading.common.gateway.ApiType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 네이버 검색 API 서비스.
 * 장전 주도주 발굴에 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverSearchService {

    private final ApiGatekeeper gatekeeper;

    @Value("${naver.api.client-id:}")
    private String clientId;

    @Value("${naver.api.client-secret:}")
    private String clientSecret;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://openapi.naver.com")
            .build();

    /**
     * 뉴스 검색.
     * 
     * @param query 검색어 (예: "삼성전자 수주")
     * @param count 결과 수 (최대 100)
     * @return 뉴스 항목 리스트
     */
    public List<NaverNewsItem> searchNews(String query, int count) {
        log.debug("[NaverSearch] 뉴스 검색: '{}' ({}건)", query, count);

        return gatekeeper.execute(ApiType.NAVER, () -> {
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/search/news.json")
                                .queryParam("query", query)
                                .queryParam("display", Math.min(count, 100))
                                .queryParam("sort", "date")
                                .build())
                        .header("X-Naver-Client-Id", clientId)
                        .header("X-Naver-Client-Secret", clientSecret)
                        .retrieve()
                        .body(Map.class);

                return parseNewsResponse(response);
            } catch (Exception e) {
                log.error("[NaverSearch] 검색 실패: {}", query, e);
                return List.of();
            }
        });
    }

    /**
     * 장전 주도주 발굴.
     * "특징주", "급등", "수주" 등의 키워드로 검색.
     */
    public List<String> discoverLeadingStocks() {
        log.info("[NaverSearch] 장전 주도주 발굴 시작");

        var keywords = List.of("특징주", "급등 예상", "대규모 수주", "호재 공시");
        var discoveredStocks = new ArrayList<String>();

        for (var keyword : keywords) {
            var news = searchNews(keyword, 10);
            for (var item : news) {
                var stockCode = extractStockCode(item.title() + " " + item.description());
                if (stockCode != null && !discoveredStocks.contains(stockCode)) {
                    discoveredStocks.add(stockCode);
                    log.info("[NaverSearch] 발굴: {} (키워드: {})", stockCode, keyword);
                }
            }
        }

        log.info("[NaverSearch] 총 {}개 종목 발굴", discoveredStocks.size());
        return discoveredStocks;
    }

    private List<NaverNewsItem> parseNewsResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("items")) {
            return List.of();
        }

        var items = (List<Map<String, String>>) response.get("items");
        return items.stream()
                .map(item -> new NaverNewsItem(
                        stripHtml(item.get("title")),
                        stripHtml(item.get("description")),
                        item.get("link"),
                        item.get("pubDate")))
                .toList();
    }

    private String stripHtml(String text) {
        if (text == null)
            return "";
        return text.replaceAll("<[^>]*>", "").replaceAll("&[^;]+;", "");
    }

    private String extractStockCode(String content) {
        var pattern = java.util.regex.Pattern.compile("\\b(\\d{6})\\b");
        var matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 네이버 뉴스 항목 DTO.
     */
    public record NaverNewsItem(
            String title,
            String description,
            String link,
            String pubDate) {
    }
}
