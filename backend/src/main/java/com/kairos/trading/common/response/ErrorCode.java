package com.kairos.trading.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 에러 코드 정의.
 * 모든 비즈니스 예외는 이 enum을 참조하여 일관된 에러 메시지를 제공한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러 (1xxx)
    INTERNAL_SERVER_ERROR("1000", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST("1001", "잘못된 요청입니다."),
    RESOURCE_NOT_FOUND("1002", "요청한 리소스를 찾을 수 없습니다."),
    VALIDATION_FAILED("1003", "입력값 검증에 실패했습니다."),

    // 인증/인가 에러 (2xxx)
    UNAUTHORIZED("2000", "인증이 필요합니다."),
    FORBIDDEN("2001", "접근 권한이 없습니다."),
    TOKEN_EXPIRED("2002", "토큰이 만료되었습니다."),
    TOKEN_INVALID("2003", "유효하지 않은 토큰입니다."),

    // 외부 API 에러 (3xxx)
    KIWOOM_API_ERROR("3000", "키움 API 호출에 실패했습니다."),
    KIWOOM_RATE_LIMIT_EXCEEDED("3001", "키움 API 호출 제한을 초과했습니다."),
    KIWOOM_TOKEN_EXPIRED("3002", "키움 API 토큰이 만료되었습니다."),
    KIWOOM_ORDER_FAILED("3003", "키움 주문 처리에 실패했습니다."),
    NAVER_API_ERROR("3100", "네이버 API 호출에 실패했습니다."),
    NAVER_QUOTA_EXCEEDED("3101", "네이버 API 일일 할당량을 초과했습니다."),
    GEMINI_API_ERROR("3200", "Gemini API 호출에 실패했습니다."),

    // 트레이딩 에러 (4xxx)
    INSUFFICIENT_BALANCE("4000", "예수금이 부족합니다."),
    ORDER_FAILED("4001", "주문 처리에 실패했습니다."),
    MARKET_CLOSED("4002", "현재 거래 시간이 아닙니다."),
    KILL_SWITCH_ACTIVATED("4003", "Kill Switch가 발동되어 매매가 중단되었습니다."),

    // 에이전트 에러 (5xxx)
    AGENT_ANALYSIS_FAILED("5000", "에이전트 분석에 실패했습니다."),
    AGENT_TIMEOUT("5001", "에이전트 응답 시간이 초과되었습니다.");

    private final String code;
    private final String message;
}
