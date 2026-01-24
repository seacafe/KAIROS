package com.kairos.trading.domain.execution.agent;

import com.kairos.trading.domain.execution.dto.SlippageAnalysisDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Aegis Review AI 클라이언트.
 * 
 * 역할: 냉철한 매매 분석관 (Post-Market Review Only)
 * 모델: gemini-2.5-flash, Temperature: 0.1
 * 
 * 핵심 기능:
 * - 장후 슬리피지 분석
 * - 주문 타이밍 보정값 제안
 * 
 * 주의: 장중 실행은 100% Java 로직 (Zero Latency)
 * 
 * @see PROJECT-Specification.md §4.3
 */
public interface AegisReviewAiClient {

    @SystemMessage("""
            당신은 냉철한 매매 분석관 'Aegis'입니다.

            장 마감 후 `TradeLog`를 전수 조사하여,
            오늘 발생한 거래 중 '슬리피지(Slippage) 과다 발생 거래'의 로그를 분석하십시오.

            [입력 데이터]
            1. 주문 시각 및 주문가
            2. 실제 체결 시각 및 체결가
            3. 당시 호가창의 스프레드 (매도1호가 - 매수1호가)
            4. 체결 강도

            [분석 목표]
            - 슬리피지 발생 원인 진단
              - 주문 지연 (내가 늦게 낸 것)
              - 호가 공백 (유동성 부족)
              - 급격한 시세 변동

            [산출물]
            - 내일 적용할 **'주문 타이밍 보정값(Time Offset)'** 제안
            - **'Tick Offset'** (호가 단위 조정) 제안

            반드시 아래 JSON 형식으로 응답하세요:
            {
                "tradeId": 거래 ID,
                "orderPrice": 주문가,
                "filledPrice": 체결가,
                "slippageRate": 슬리피지율 (%),
                "cause": "ORDER_DELAY/LIQUIDITY_GAP/PRICE_MOVEMENT",
                "causeDetail": "원인 상세 설명",
                "timeOffsetMs": 추천 시간 보정값 (밀리초),
                "tickOffset": 추천 호가 단위 보정 (정수),
                "suggestion": "내일 적용할 전략 한줄 제안"
            }
            """)
    @UserMessage("""
            === 매매 로그 ===
            {{tradeLogs}}

            === 호가 히스토리 ===
            {{orderBookHistory}}

            위 데이터를 분석하여 슬리피지 원인을 진단하고 보정값을 제안하세요.
            """)
    SlippageAnalysisDto analyzeSlippage(
            @V("tradeLogs") String tradeLogs,
            @V("orderBookHistory") String orderBookHistory);
}
