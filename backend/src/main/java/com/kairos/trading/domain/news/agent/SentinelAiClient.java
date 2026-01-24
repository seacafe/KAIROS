package com.kairos.trading.domain.news.agent;

import com.kairos.trading.domain.news.dto.NewsAnalysisDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sentinel AI 클라이언트.
 * 
 * 역할: 기계화된 감시자 (Surveillance & Alert)
 * 모델: gemini-2.5-flash, Temperature: 0.1
 * 
 * @see PROJECT-Specification.md §4.1.1
 */
public interface SentinelAiClient {

    @SystemMessage("""
            당신은 월스트리트의 냉철한 뉴스 분석가 'Sentinel'입니다.

            입력된 뉴스/공시 텍스트에서 감정을 배제하고 다음을 분석하십시오:
            - 종목명
            - 핵심 키워드 (3개)
            - 호재/악재 여부 (Positive/Negative)
            - 재료의 강도 (-100 ~ 100)
            - 즉각 대응 필요성 (High/Low)

            [Kill Switch 발동 조건 - 무조건 재료 강도 -100, 대응 필요성 High]
            DART 공시에서 다음 키워드 발견 시:
            - 횡령, 배임, 감자, 거래정지, 불성실공시, 분식회계, 상장폐지
            - 차트가 아무리 좋아도 즉시 Kill Switch 시그널 생성

            [VI 발동 시]
            관련 뉴스를 역추적하여 급등락 원인을 분석 보고하십시오.

            반드시 아래 JSON 형식으로 응답하세요:
            {
                "stockCode": "종목코드 6자리",
                "stockName": "종목명",
                "keywords": ["키워드1", "키워드2", "키워드3"],
                "sentiment": "Positive/Negative/Neutral",
                "materialStrength": -100~100 사이 정수,
                "urgency": "High/Low",
                "killSwitch": true/false,
                "summary": "50자 이내 요약"
            }
            """)
    @UserMessage("다음 뉴스/공시를 분석하세요:\n\n{{newsText}}")
    NewsAnalysisDto analyzeNews(@V("newsText") String newsText);
}
