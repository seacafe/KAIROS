package com.kairos.trading.domain.sentiment.agent;

import com.kairos.trading.domain.sentiment.dto.MarketSentimentDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Resonance AI 클라이언트.
 * 
 * 역할: 거시경제학자 (Market Psychologist)
 * 모델: gemini-2.5-flash, Temperature: 0.6 (뉘앙스 파악)
 * 
 * 핵심 기능:
 * - Market Heat Score (0~100) 산출
 * - Risk-On/Risk-Off 판단
 * - 신규 진입 차단(Veto) 권한
 * 
 * @see PROJECT-Specification.md §4.1.4
 */
public interface ResonanceAiClient {

    @SystemMessage("""
            당신은 거시경제학자 'Resonance'입니다.

            미국 나스닥 선물 지수, 주요 원자재 가격, 환율, 유가 및 뉴스 헤드라인의 어조,
            커뮤니티 반응, 섹터 수급 쏠림 현상 등을 분석 및 종합하여
            'Market Heat Score(0~100)'를 산출하십시오.

            [판단 기준]
            - 나스닥 선물 급락 → 점수 하락
            - 환율(USD/KRW) 급등 → 점수 하락
            - VIX 30 이상 → 공포 상태

            [Veto 발동 기준 - 신규 진입 차단]
            - Stable 성향: 50점 미만
            - Neutral 성향: 40점 미만
            - Aggressive 성향: 30점 미만

            [권고]
            시장 전체가 공포에 질려 있다면(40점 미만),
            개별 종목의 호재를 무시하고 '보수적 대응'을 강력히 권고하십시오.

            반드시 아래 JSON 형식으로 응답하세요:
            {
                "marketHeatScore": 0~100 사이 정수,
                "sentiment": "Extreme Greed/Greed/Neutral/Fear/Extreme Fear",
                "nasdaqImpact": "나스닥 영향 한줄 설명",
                "currencyImpact": "환율 영향 한줄 설명",
                "vixImpact": "VIX 영향 한줄 설명",
                "riskStatus": "RISK_ON/RISK_OFF",
                "vetoThreshold": {"stable": 50, "neutral": 40, "aggressive": 30},
                "summary": "50자 이내 시장 심리 요약"
            }
            """)
    @UserMessage("""
            글로벌 시장 데이터:
            - 나스닥 선물: {{nasdaqChange}}%
            - 달러/원 환율: {{usdKrw}}원
            - VIX 지수: {{vix}}
            - 코스피 선물: {{kospiChange}}%
            - 유가(WTI): {{oilPrice}}$

            뉴스 헤드라인 분위기:
            {{newsHeadlines}}

            위 데이터를 기반으로 시장 심리를 분석하세요.
            """)
    MarketSentimentDto analyzeMarket(
            @V("nasdaqChange") double nasdaqChange,
            @V("usdKrw") double usdKrw,
            @V("vix") double vix,
            @V("kospiChange") double kospiChange,
            @V("oilPrice") double oilPrice,
            @V("newsHeadlines") String newsHeadlines);
}
