package com.kairos.trading.common.ai;

import java.util.Map;

/**
 * 에이전트 분석 결과 공통 DTO.
 * 모든 에이전트(Sentinel, Axiom, Vector, Resonance, Sonar, Nexus)가 이 형식으로 결과를 반환한다.
 * 
 * @param agentName 에이전트 이름 (Sentinel, Axiom 등)
 * @param score     분석 점수 (0~100)
 * @param decision  판단 결과 (BUY, WATCH, REJECT, ALERT)
 * @param reason    판단 근거 (요약)
 * @param metadata  추가 데이터 (에이전트별 상세 정보)
 */
public record AgentResponse(
        String agentName,
        int score,
        String decision,
        String reason,
        Map<String, Object> metadata) {
    /**
     * 긍정적 판단인지 확인 (BUY)
     */
    public boolean isPositive() {
        return "BUY".equals(decision);
    }

    /**
     * 중립적 판단인지 확인 (WATCH)
     */
    public boolean isNeutral() {
        return "WATCH".equals(decision);
    }

    /**
     * 부정적 판단인지 확인 (REJECT)
     */
    public boolean isNegative() {
        return "REJECT".equals(decision);
    }

    /**
     * 긴급 경고인지 확인 (ALERT - Kill Switch 등)
     */
    public boolean isAlert() {
        return "ALERT".equals(decision);
    }

    /**
     * 점수 기반 등급 반환
     */
    public String getGrade() {
        if (score >= 80)
            return "A";
        if (score >= 60)
            return "B";
        if (score >= 40)
            return "C";
        if (score >= 20)
            return "D";
        return "F";
    }
}
