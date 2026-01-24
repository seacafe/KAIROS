package com.kairos.trading.domain.news.service;

import com.kairos.trading.common.event.KillSwitchEvent;
import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import com.kairos.trading.domain.settings.entity.RssFeed;
import com.kairos.trading.domain.settings.repository.RssFeedRepository;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSS 피드 모니터링 서비스.
 * 
 * 역할:
 * 1. 등록된 RSS 피드 주기적 폴링 (5분)
 * 2. DART 공시 키워드 감지 시 KillSwitch 발행
 * 3. 주도 테마 키워드 감지 시 분석 트리거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssMonitoringService {

    private final RssFeedRepository rssFeedRepository;
    private final SentinelService sentinelService;
    private final ApplicationEventPublisher eventPublisher;

    // 이미 처리한 뉴스 ID (중복 방지)
    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    // 마지막 폴링 시간
    private final Map<Long, LocalDateTime> lastPolledAt = new ConcurrentHashMap<>();

    /**
     * RSS 피드 주기적 폴링 (5분 간격).
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000) // 5분
    public void pollRssFeeds() {
        log.debug("[RssMonitor] RSS 피드 폴링 시작");

        List<RssFeed> activeFeeds = rssFeedRepository.findByIsActiveTrueOrderByIdAsc();

        for (RssFeed feed : activeFeeds) {
            try {
                pollFeed(feed);
            } catch (Exception e) {
                log.error("[RssMonitor] 피드 폴링 실패: {} - {}", feed.getName(), e.getMessage());
            }
        }
    }

    /**
     * 단일 피드 폴링.
     */
    private void pollFeed(RssFeed feed) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed syndFeed = input.build(new XmlReader(new URL(feed.getUrl())));

        LocalDateTime lastPolled = lastPolledAt.getOrDefault(feed.getId(), LocalDateTime.MIN);
        List<SyndEntry> newEntries = new ArrayList<>();

        for (SyndEntry entry : syndFeed.getEntries()) {
            String entryId = entry.getUri() != null ? entry.getUri() : entry.getLink();

            // 중복 체크
            if (processedIds.contains(entryId)) {
                continue;
            }

            // 시간 체크 (마지막 폴링 이후)
            if (entry.getPublishedDate() != null) {
                LocalDateTime publishedAt = entry.getPublishedDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                if (publishedAt.isBefore(lastPolled)) {
                    continue;
                }
            }

            newEntries.add(entry);
            processedIds.add(entryId);
        }

        // 새 뉴스 처리
        for (SyndEntry entry : newEntries) {
            processEntry(feed, entry);
        }

        lastPolledAt.put(feed.getId(), LocalDateTime.now());

        if (!newEntries.isEmpty()) {
            log.info("[RssMonitor] {} - 새 뉴스 {}건", feed.getName(), newEntries.size());
        }
    }

    /**
     * 개별 뉴스 항목 처리.
     */
    private void processEntry(RssFeed feed, SyndEntry entry) {
        String title = entry.getTitle();
        String content = entry.getDescription() != null ? entry.getDescription().getValue() : "";
        String fullText = title + " " + content;

        log.debug("[RssMonitor] 뉴스 수신: {}", title);

        // 1. Kill Switch 키워드 사전 체크
        if (sentinelService.containsKillSwitchKeyword(fullText)) {
            log.error("[RssMonitor] ⚠️ KILL SWITCH 키워드 감지: {}", title);

            // 종목 코드 추출 시도 (공시의 경우)
            String stockCode = extractStockCode(fullText);
            String stockName = extractStockName(fullText);

            if (stockCode != null) {
                eventPublisher.publishEvent(new KillSwitchEvent(
                        this,
                        stockCode,
                        stockName != null ? stockName : stockCode,
                        title,
                        "RSS:" + feed.getName()));
            }
            return;
        }

        // 2. AI 분석 (비동기)
        if ("DISCLOSURE".equals(feed.getCategory())) {
            // DART 공시는 상세 분석
            NewsAnalysisDto result = sentinelService.analyzeNews(fullText);

            if (result.requiresKillSwitch()) {
                eventPublisher.publishEvent(new KillSwitchEvent(
                        this,
                        result.stockCode(),
                        result.stockName(),
                        result.summary(),
                        "Sentinel"));
            }
        }
    }

    /**
     * 텍스트에서 종목 코드 추출 (6자리 숫자).
     */
    private String extractStockCode(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{6})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 텍스트에서 종목명 추출 (대괄호 내용).
     */
    private String extractStockName(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 처리된 ID 캐시 정리 (하루 지난 항목).
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void cleanupProcessedIds() {
        int beforeSize = processedIds.size();
        processedIds.clear();
        log.info("[RssMonitor] 처리 ID 캐시 정리: {} → 0", beforeSize);
    }
}
