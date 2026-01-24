package com.kairos.trading.common.exception;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전역 예외 처리기.
 * 모든 컨트롤러에서 발생하는 예외를 포착하여 BaseResponse 형식으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("비즈니스 예외 발생: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error(e.getErrorCode()));
    }

    /**
     * Validation 예외 처리 (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        var errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("검증 오류");

        log.warn("검증 실패: {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error(errorMessage));
    }

    /**
     * Binding 예외 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<Void>> handleBindException(BindException e) {
        var errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("바인딩 오류");

        log.warn("바인딩 실패: {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error(errorMessage));
    }

    /**
     * 리소스 Not Found 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFoundException(NoHandlerFoundException e) {
        log.warn("리소스를 찾을 수 없음: {}", e.getRequestURL());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /**
     * 최상위 예외 처리 (예상치 못한 오류)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        log.error("예상치 못한 서버 오류 발생", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
