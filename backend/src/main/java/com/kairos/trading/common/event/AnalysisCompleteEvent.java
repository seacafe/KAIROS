package com.kairos.trading.common.event;

import com.kairos.trading.common.ai.AgentResponse;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 분석 완료 이벤트.
 * 5인 에이전트 분석이 완료되면 발행하며, Nexus가 구독하여 의사결정을 수행한다.
 */
public class AnalysisCompleteEvent extends ApplicationEvent {

    private final String stockCode;
    private final String stockName;
    private final List<AgentResponse> reports;
    private String analysisType;
    private int score;
    private long price;
    private String description;

    // Constructor for Multi-Agent Analysis (Nexus)
    public AnalysisCompleteEvent(Object source, String stockCode, String stockName, List<AgentResponse> reports) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.reports = reports;
        this.analysisType = "COMPREHENSIVE";
        this.score = getAverageScore();
    }

    // Constructor for Single Agent Analysis (TradingEventListener)
    public AnalysisCompleteEvent(Object source, String stockCode, String stockName, String analysisType, int score,
            long price, String description) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.analysisType = analysisType;
        this.score = score;
        this.price = price;
        this.description = description;
        this.reports = null;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public int getScore() {
        return score;
    }

    public long getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public List<AgentResponse> getReports() {
        return reports;
    }

    public int getAverageScore() {
        if (reports == null || reports.isEmpty())
            return 0;
        return (int) reports.stream()
                .mapToInt(AgentResponse::score)
                .average()
                .orElse(0);
    }
}
