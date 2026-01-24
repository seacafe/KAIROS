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

    public AnalysisCompleteEvent(Object source, String stockCode, String stockName, List<AgentResponse> reports) {
        super(source);
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.reports = reports;
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
