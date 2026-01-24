package com.kairos.trading.common.gateway;

/**
 * 외부 API 타입 정의.
 * ApiGatekeeper에서 API별 Rate Limit을 적용할 때 사용한다.
 */
public enum ApiType {

    /**
     * 키움증권 API
     * - Rate Limit: 4 req/sec (Strict - Leaky Bucket)
     * - 초과 시 계좌 동결(Ban) 위험
     */
    KIWOOM,

    /**
     * 네이버 검색 API
     * - Rate Limit: 10 req/sec + 일일 25,000건 Quota
     * - 장전 Burst 허용, 장중 보수적 사용
     */
    NAVER,

    /**
     * Google Gemini API
     * - Rate Limit: 1,000 req/min (Pay-as-you-go)
     * - Cost Safety Cap 적용
     */
    GEMINI
}
