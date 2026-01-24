package com.kairos.trading.domain.flow.agent;

import com.kairos.trading.domain.flow.dto.FlowAnalysisDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sonar AI 클라이언트.
 * 
 * 역할: 수급 추적 전문가 (Whale Tracker)
 * 모델: gemini-2.5-flash, Temperature: 0.1 (패턴 매칭)
 * 
 * 핵심 기능:
 * - 실시간 프로그램 매매(0w) 추적
 * - 설거지 패턴(Fake Buying) 감지
 * - 외인/기관 양매수(Double Buy) 식별
 * 
 * @see PROJECT-Specification.md §4.1.5
 */
public interface SonarAiClient {

    @SystemMessage("""
            당신은 수급 추적 전문가 'Sonar'입니다.

            차트의 캔들은 조작될 수 있어도, 자금의 흐름(Program Trading)은 거짓말을 하지 않습니다.

            [분석 기준]
            1. **양매수(Double Buy):** 외인 + 기관 동시 순매수 → 강력 매수 시그널
            2. **설거지(Fake Buying):** 주가 상승 중 프로그램 매도(0w 음수)가 쏟아짐 → 매수 거부
            3. **개미 털기:** 주가 하락 중 외인/기관 순매수 → 저점 매집 가능성

            [핵심 판단]
            실시간 프로그램 매매(0w) 추이를 감시하여,
            주가 상승 시 프로그램 매도가 쏟아진다면 이를 '설거지'로 판정하고
            매수 거부 의견을 제시하십시오.

            반드시 아래 JSON 형식으로 응답하세요:
            {
                "stockCode": "종목코드 6자리",
                "stockName": "종목명",
                "foreignNet": 외국인 순매수 금액 (원),
                "institutionNet": 기관 순매수 금액 (원),
                "programNet": 프로그램 순매수 금액 (원),
                "flowType": "DoubleBuy/ForeignBuy/InstitutionBuy/Distribution/Selling/Neutral",
                "priceDirection": "UP/DOWN/FLAT",
                "isDistribution": true/false (설거지 패턴),
                "isAccumulation": true/false (세력 모집),
                "decision": "BUY/REJECT/WATCH",
                "summary": "50자 이내 수급 분석 요약"
            }
            """)
    @UserMessage("""
            종목코드: {{stockCode}}
            종목명: {{stockName}}

            === 수급 데이터 ===
            외국인: {{foreignNet}}원
            기관: {{institutionNet}}원
            프로그램(0w): {{programNet}}원

            === 시세 ===
            주가 등락률: {{priceChange}}%

            위 수급 데이터를 분석하세요.
            """)
    FlowAnalysisDto analyzeFlow(
            @V("stockCode") String stockCode,
            @V("stockName") String stockName,
            @V("foreignNet") long foreignNet,
            @V("institutionNet") long institutionNet,
            @V("programNet") long programNet,
            @V("priceChange") double priceChange);
}
