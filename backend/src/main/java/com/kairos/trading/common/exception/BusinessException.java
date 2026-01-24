package com.kairos.trading.common.exception;

import com.kairos.trading.common.response.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 커스텀 예외.
 * 모든 도메인 예외는 이 클래스를 상속받거나 직접 사용한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + " - " + additionalMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
