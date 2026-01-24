package com.kairos.trading.common.ai;

import java.util.Map;

/**
 * Mock AI 클라이언트 (테스트용).
 * 실제 Gemini API를 호출하지 않고 고정된 응답을 반환한다.
 */
public class MockAiClient {

    /**
     * Sentinel 에이전트용 Mock 응답
     */
    public static AgentResponse mockSentinelResponse(boolean isPositive) {
        if (isPositive) {
            return new AgentResponse(
                    "Sentinel",
                    85,
                    "BUY",
                    "삼성전자, 반도체 수주 대형 호재. HBM 수요 급증 전망.",
                    Map.of(
                            "keywords", new String[] { "HBM", "수주", "반도체" },
                            "sentiment", "Positive",
                            "urgency", "Low"));
        } else {
            return new AgentResponse(
                    "Sentinel",
                    -100,
                    "ALERT",
                    "횡령 공시 감지. 즉시 Kill Switch 발동 필요.",
                    Map.of(
                            "keywords", new String[] { "횡령", "배임" },
                            "sentiment", "Negative",
                            "urgency", "High",
                            "killSwitch", true));
        }
    }

    /**
     * Axiom 에이전트용 Mock 응답
     */
    public static AgentResponse mockAxiomResponse(boolean isHealthy) {
        if (isHealthy) {
            return new AgentResponse(
                    "Axiom",
                    75,
                    "BUY",
                    "재무 건전성 양호. 영업이익 흑자 전환, 부채비율 정상.",
                    Map.of(
                            "per", 12.5,
                            "pbr", 1.2,
                            "debtRatio", 45.3,
                            "operatingProfit", true));
        } else {
            return new AgentResponse(
                    "Axiom",
                    20,
                    "REJECT",
                    "3년 연속 적자. 부채비율 300% 초과. 상장폐지 위험.",
                    Map.of(
                            "per", -5.0,
                            "pbr", 0.3,
                            "debtRatio", 350.0,
                            "operatingProfit", false,
                            "consecutiveLoss", 3));
        }
    }

    /**
     * Vector 에이전트용 Mock 응답
     */
    public static AgentResponse mockVectorResponse(boolean isBullish) {
        if (isBullish) {
            return new AgentResponse(
                    "Vector",
                    90,
                    "BUY",
                    "NanoBanana 패턴 확인. 이평선 정배열, 거래량 폭발.",
                    Map.of(
                            "pattern", "NanoBanana",
                            "entryPrice", 72000,
                            "targetPrice", 78000,
                            "stopLoss", 69500,
                            "volumeRatio", 2.5));
        } else {
            return new AgentResponse(
                    "Vector",
                    35,
                    "WATCH",
                    "이평선 역배열. 추세 확인 필요.",
                    Map.of(
                            "pattern", "Bearish",
                            "resistance", 73000,
                            "support", 68000));
        }
    }

    /**
     * Resonance 에이전트용 Mock 응답
     */
    public static AgentResponse mockResonanceResponse(int marketScore) {
        String decision = marketScore >= 50 ? "BUY" : "WATCH";
        String reason = marketScore >= 50
                ? "시장 심리 양호. Risk-On 모드."
                : "시장 불안. 현금 비중 확대 권고.";

        return new AgentResponse(
                "Resonance",
                marketScore,
                decision,
                reason,
                Map.of(
                        "marketHeatScore", marketScore,
                        "nasdaqFutures", 0.5,
                        "fearGreedIndex", marketScore > 50 ? "Greed" : "Fear"));
    }

    /**
     * Sonar 에이전트용 Mock 응답
     */
    public static AgentResponse mockSonarResponse(boolean isInstitutionalBuy) {
        if (isInstitutionalBuy) {
            return new AgentResponse(
                    "Sonar",
                    80,
                    "BUY",
                    "외인/기관 양매수. 프로그램 매수 우세.",
                    Map.of(
                            "foreignNet", 50000000000L,
                            "institutionNet", 30000000000L,
                            "programNet", 10000000000L,
                            "flowType", "DoubleBuy"));
        } else {
            return new AgentResponse(
                    "Sonar",
                    30,
                    "REJECT",
                    "주가 상승 중 프로그램 매도 급증. 설거지 패턴.",
                    Map.of(
                            "foreignNet", -20000000000L,
                            "institutionNet", -15000000000L,
                            "programNet", -25000000000L,
                            "flowType", "Distribution"));
        }
    }

    /**
     * Nexus 전략가용 Mock 응답
     */
    public static AgentResponse mockNexusResponse(String strategyMode, int avgScore) {
        String decision;
        String riskLevel;

        switch (strategyMode) {
            case "AGGRESSIVE" -> {
                decision = avgScore >= 50 ? "BUY" : "WATCH";
                riskLevel = "HIGH";
            }
            case "STABLE" -> {
                decision = avgScore >= 70 ? "BUY" : "REJECT";
                riskLevel = "LOW";
            }
            default -> {
                decision = avgScore >= 60 ? "BUY" : "WATCH";
                riskLevel = "MEDIUM";
            }
        }

        return new AgentResponse(
                "Nexus",
                avgScore,
                decision,
                String.format("[%s 모드] 종합 점수 %d점. %s 판정.", strategyMode, avgScore, decision),
                Map.of(
                        "strategyMode", strategyMode,
                        "riskLevel", riskLevel,
                        "targetPrice", 78000,
                        "stopLossPrice", 69500,
                        "positionSize", 0.1));
    }
}
