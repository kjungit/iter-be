package com.example.iter.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 프로젝트 전역에서 사용하는 에러 코드 모음.
// 팀원 각자 담당 도메인 개발 중 필요한 에러 상황이 생기면 이 enum에 항목을 추가해서 사용한다.
// (도메인 패키지마다 별도 ErrorCode를 두지 않고 common에서 통합 관리 — 컨벤션 문서 기준)
@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth (A. 곽성현 담당 영역)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    SUSPENDED_USER(HttpStatus.FORBIDDEN, "정지된 계정입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // Device (A. 곽성현 담당 영역)
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장비입니다."),
    EQUIPMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "본인 소유의 장비가 아닙니다."),
    EQUIPMENT_NOT_AVAILABLE(HttpStatus.CONFLICT, "대여할 수 없는 상태의 장비입니다."),

    // Admin (C)
    ADMIN_SUSPENSION_NOT_ALLOWED(HttpStatus.FORBIDDEN,"관리자 계정은 정지할 수 없습니다."),
    INVALID_USER_STATUS_TRANSITION(HttpStatus.CONFLICT,"허용되지 않는 회원 상태 변경입니다."),

    // Reservation - Return (C)
    RENTAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 대여 거래입니다."),
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "수령 증빙을 찾을 수 없습니다."),
    RETURN_RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "반납 증빙을 찾을 수 없습니다."),
    RETURN_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 최종 확인된 반납 거래입니다."),
    INVALID_RETURN_CONFIRMATION_STATUS(HttpStatus.CONFLICT, "반납 도착 확인 상태의 거래만 최종 확인할 수 있습니다.");

    // TODO: reservation / payment / delivery / dispute 담당자가 각자 도메인 에러코드를 이어서 추가

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
