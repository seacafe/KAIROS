package com.kairos.trading.domain.news.service;

import com.kairos.trading.common.event.KillSwitchEvent;

import com.kairos.trading.domain.news.entity.RssFeedConfig;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * RSS 피드 서비스.
 * DART 공시, 뉴스를 주기적으로 폴링하여 Kill Switch 키워드를 감지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssFeedService {

    private final ApplicationEventPublisher eventPublisher;

    // 활성 RSS 피드 목록
    private final List<RssFeedConfig> activeFeeds = new ArrayList<>();

    // 이미 처리한 항목 (중복 방지)
    private final Set<String> processedGuids = Collections.synchronizedSet(new HashSet<>());

    // Kill Switch 키워드
    private static final List<String> KILL_SWITCH_KEYWORDS = List.of(
            "횡령", "배임", "감자", "상장폐지", "거래정지", "불성실공시", "분식회계",
            "검찰", "수사", "압수수색", "자본잠식", "청산");

    /**
     * RSS 피드 등록.
     */
    public void registerFeed(RssFeedConfig config) {
        activeFeeds.add(config);
        log.info("[RssFeed] 피드 등록: {} ({})", config.getName(), config.getCategory());
    }

    /**
     * 5분 주기로 RSS 피드 폴링. (장중 09:00 ~ 16:00)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // 5분
    public void pollFeeds() {
        if (!isMarketHours()) {
            return;
        }

        log.debug("[RssFeed] 피드 폴링 시작 ({} 개)", activeFeeds.size());

        for (var feedConfig : activeFeeds) {
            if (!feedConfig.getIsActive()) {
                continue;
            }

            try {
                pollSingleFeed(feedConfig);
            } catch (Exception e) {
                log.error("[RssFeed] 피드 폴링 실패: {}", feedConfig.getUrl(), e);
            }
        }
    }

    /**
     * 단일 피드 폴링.
     */
    private void pollSingleFeed(RssFeedConfig config) throws Exception {
        var input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(config.getUrl())));

        for (SyndEntry entry : feed.getEntries()) {
            var guid = entry.getUri();

            // 이미 처리한 항목 스킵
            if (processedGuids.contains(guid)) {
                continue;
            }

            // 최근 10분 내 항목만 처리
            var publishedDate = entry.getPublishedDate();
            if (publishedDate != null) {
                var publishedTime = publishedDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                if (publishedTime.isBefore(LocalDateTime.now().minusMinutes(10))) {
                    continue;
                }
            }

            processedGuids.add(guid);
            processEntry(entry, config);
        }
    }

    /**
     * RSS 항목 처리.
     */
    private void processEntry(SyndEntry entry, RssFeedConfig config) {
        var title = entry.getTitle();
        var description = entry.getDescription() != null ? entry.getDescription().getValue() : "";
        var content = title + " " + description;

        log.debug("[RssFeed] 새 항목: {}", title);

        // Kill Switch 키워드 검사
        for (var keyword : KILL_SWITCH_KEYWORDS) {
            if (content.contains(keyword)) {
                log.warn("[RssFeed] ⚠️ Kill Switch 키워드 감지: '{}' in '{}'", keyword, title);

                // 종목 코드 추출 시도
                var stockCode = extractStockCode(content);
                var stockName = extractStockName(title);

                if (stockCode != null) {
                    eventPublisher.publishEvent(new KillSwitchEvent(
                            this,
                            stockCode,
                            stockName,
                            "RSS 공시: " + title,
                            "RssFeedService"));
                }
                break;
            }
        }
    }

    /**
     * 본문에서 종목 코드 추출 (6자리 숫자).
     */
    private String extractStockCode(String content) {
        var pattern = java.util.regex.Pattern.compile("\\b(\\d{6})\\b");
        var matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 제목에서 종목명 추출 (대괄호 내용).
     */
    private String extractStockName(String title) {
        var pattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        var matcher = pattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
    }

    /**
     * 장 시간인지 확인 (09:00 ~ 16:00).
     */
    private boolean isMarketHours() {
        var now = LocalDateTime.now();
        var hour = now.getHour();
        return hour >= 9 && hour < 16;
    }

    /**
     * 처리된 GUID 캐시 초기화 (일별).
     */
    public void clearProcessedGuids() {
        processedGuids.clear();
        log.info("[RssFeed] 처리 캐시 초기화");
    }
}
