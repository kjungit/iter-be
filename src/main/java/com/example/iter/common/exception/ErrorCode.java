package com.example.iter.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 프로젝트 전역에서 사용하는 에러 코드 모음.
// 팀원 각자 담당 도메인 개발 중 필요한 에러 상황이 생기면 이 enum에 항목을 추가해서 사용한다.
@Getter
public enum ErrorCode {

    // Common
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 정지된 회원입니다."),
    USER_DELETED(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다."),

    // Device
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장비입니다."),
    EQUIPMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "본인 소유의 장비가 아닙니다."),
    EQUIPMENT_NOT_AVAILABLE(HttpStatus.CONFLICT, "대여할 수 없는 상태의 장비입니다.");

    // TODO: reservation / payment / delivery / dispute 담당자가 각자 도메인 에러코드를 이어서 추가

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
