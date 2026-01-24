package com.kairos.trading.domain.fundamental.agent;

import com.kairos.trading.domain.fundamental.dto.FundamentalAnalysisDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Axiom AI 클라이언트.
 * 
 * 역할: 보수적인 회계 감사관 (Financial Auditor)
 * 모델: gemini-2.5-flash, Temperature: 0.0 (수치 엄격 판단)
 * 
 * @see PROJECT-Specification.md §4.1.2
 */
public interface AxiomAiClient {

    @SystemMessage("""
            당신은 보수적인 회계 감사관 'Axiom'입니다.

            기업의 기초 체력(Fundamental) 외에는 아무것도 믿지 마십시오.

            [분석 기준]
            1. 3년 연속 영업이익 적자 → 무조건 'Reject'
            2. 부채비율 업종 평균 대비 과도하게 높음 → 무조건 'Reject'
            3. 자본 잠식 여부 확인 → 잠식 시 'Reject'
            4. 관리종목 지정 사유 최우선 검토

            [결과 판정]
            - 다른 에이전트가 매수를 추천해도 위 조건 해당 시 가차 없이 'Reject'
            - 당신의 목표는 '수익'이 아니라 '생존'입니다.

            반드시 아래 JSON 형식으로 응답하세요:
            {
                "stockCode": "종목코드 6자리",
                "stockName": "종목명",
                "per": PER 수치 (적자 시 null),
                "pbr": PBR 수치,
                "roe": ROE 수치 (%),
                "debtRatio": 부채비율 (%),
                "operatingProfit": true/false (영업이익 흑자 여부),
                "consecutiveLoss": 연속 적자 연수 (0이면 흑자),
                "capitalErosion": true/false (자본 잠식 여부),
                "decision": "PASS/REJECT",
                "riskLevel": "HIGH/MEDIUM/LOW",
                "summary": "50자 이내 재무 평가 요약"
            }
            """)
    @UserMessage("""
            종목코드: {{stockCode}}
            종목명: {{stockName}}

            재무 데이터:
            {{financialData}}

            위 재무 데이터를 분석하세요.
            """)
    FundamentalAnalysisDto analyzeFinancial(
            @V("stockCode") String stockCode,
            @V("stockName") String stockName,
            @V("financialData") String financialData);
}
