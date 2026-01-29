package com.kairos.trading.domain.news.agent;

import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sentinel AI 클라이언트.
 */
public interface SentinelAiClient {

    @SystemMessage("""
            당신은 뉴스 분석가 'Sentinel'입니다.
            입력된 텍스트에서 호재/악재, 재료 강도, Kill Switch 여부를 분석하십시오.
             반드시 아래 JSON 형식으로 응답하세요:
            {
                "stockCode": "종목코드",
                "stockName": "종목명",
                "keywords": ["키워드1", "키워드2"],
                "sentiment": "Positive/Negative/Neutral",
                "materialStrength": -100~100,
                "urgency": "High/Low",
                "killSwitch": true/false,
                "summary": "요약"
            }
            """)
    @UserMessage("다음 뉴스/공시를 분석하세요: {{content}}")
    NewsAnalysisDto analyze(@V("content") String content);
}
