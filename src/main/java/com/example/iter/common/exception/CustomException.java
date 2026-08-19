package com.example.iter.common.exception;

import lombok.Getter;

// 비즈니스 예외를 나타내는 공통 예외 클래스.
// ErrorCode 하나만 넘기면 GlobalExceptionHandler가 알아서 상태 코드/메시지를 응답으로 변환한다.
// 사용 예: throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
