package com.kairos.trading.common.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 공통 API 응답 포맷.
 * 모든 Controller는 이 record를 사용하여 응답을 반환한다.
 * 
 * @param status    SUCCESS 또는 ERROR
 * @param data      응답 데이터 (nullable)
 * @param message   응답 메시지 (nullable)
 * @param timestamp ISO-8601 형식의 응답 시각
 */
public record BaseResponse<T>(
        String status,
        T data,
        String message,
        String timestamp) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(
                "SUCCESS",
                data,
                null,
                LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 성공 응답 생성 (메시지 포함)
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(
                "SUCCESS",
                data,
                message,
                LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 에러 응답 생성
     */
    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(
                "ERROR",
                null,
                message,
                LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 에러 응답 생성 (ErrorCode 사용)
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(
                "ERROR",
                null,
                errorCode.getMessage(),
                LocalDateTime.now().format(FORMATTER));
    }
}
