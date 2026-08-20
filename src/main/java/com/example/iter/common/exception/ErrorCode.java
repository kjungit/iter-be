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
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_NOT_SET(HttpStatus.CONFLICT, "비밀번호가 설정되어 있지 않은 계정입니다."),
    SAME_PASSWORD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
    ACTIVE_RENTAL_EXISTS(HttpStatus.CONFLICT, "진행 중인 대여 거래가 있어 탈퇴할 수 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 정지된 회원입니다."),
    USER_DELETED(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다."),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "기본 배송지를 찾을 수 없습니다."),
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
    OAUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 OAuth 일회용 토큰입니다."),
    OAUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 OAuth 일회용 토큰입니다."),
    OAUTH_TOKEN_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용한 OAuth 일회용 토큰입니다."),
    OAUTH_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연결된 OAuth 계정입니다."),
    OAUTH_LINK_TARGET_MISMATCH(HttpStatus.FORBIDDEN, "해당 회원에게 발급된 계정 연결 요청이 아닙니다."),
    OAUTH_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "카카오 계정 이메일과 가입 이메일이 일치하지 않습니다."),

    // Device
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않거나 조회할 수 없는 장비입니다."),
    EQUIPMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "본인 소유의 장비가 아닙니다."),
    EQUIPMENT_NOT_AVAILABLE(HttpStatus.CONFLICT, "대여할 수 없는 상태의 장비입니다."),
    EQUIPMENT_RENTAL_PERIOD_UNAVAILABLE(HttpStatus.CONFLICT, "선택한 기간에는 장비를 대여할 수 없습니다."),

    // Admin (C)
    ADMIN_SUSPENSION_NOT_ALLOWED(HttpStatus.FORBIDDEN,"관리자 계정은 정지할 수 없습니다."),
    INVALID_USER_STATUS_TRANSITION(HttpStatus.CONFLICT,"허용되지 않는 회원 상태 변경입니다."),
    INVALID_EQUIPMENT_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 장비 상태 변경입니다."),
    INVALID_REPORT_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 신고 상태 변경입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제 기록입니다."),

    // Reservation - Return (C)
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "수령 증빙을 찾을 수 없습니다."),
    RETURN_RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "반납 증빙을 찾을 수 없습니다."),
    RETURN_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 최종 확인된 반납 거래입니다."),
    INVALID_RETURN_CONFIRMATION_STATUS(HttpStatus.CONFLICT, "반납 도착 확인 상태의 거래만 최종 확인할 수 있습니다."),

    // Report (C)
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않거나 조회할 수 없는 신고입니다."),
    DUPLICATE_ACTIVE_REPORT(HttpStatus.CONFLICT, "같은 대상에 처리 중인 신고가 이미 존재합니다."),
    REPORT_SELF_TARGET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신 또는 본인 소유 대상은 신고할 수 없습니다."),

    // TODO: reservation / payment / delivery / dispute 담당자가 각자 도메인 에러코드를 이어서 추가
    // Reservation / Payment (B 담당 영역)
    EQUIPMENT_SELF_RENTAL(HttpStatus.FORBIDDEN, "본인이 등록한 장비는 대여할 수 없습니다."),
    RENTAL_PERIOD_CONFLICT(HttpStatus.CONFLICT, "선택한 기간은 이미 다른 예약(요청 포함)이 있어 진행할 수 없습니다."),
    RENTAL_NOT_FOUND(HttpStatus.NOT_FOUND, "예약 정보를 찾을 수 없습니다."),
    RENTAL_NOT_PARTY(HttpStatus.FORBIDDEN, "거래 당사자만 조회할 수 있습니다."),
    RENTAL_NOT_PAYABLE(HttpStatus.CONFLICT, "결제 대기 상태의 예약만 결제할 수 있습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 결제가 완료된 예약입니다."),
    RENTAL_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "승인 이후 예약은 취소할 수 없습니다."),
    RENTAL_NOT_APPROVABLE(HttpStatus.CONFLICT, "승인 대기 상태의 예약만 승인할 수 있습니다."),
    RENTAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 요청은 다시 처리할 수 없습니다."),
    RESERVATION_CONFLICT(HttpStatus.CONFLICT, "이미 확정된 예약과 기간이 겹쳐 승인할 수 없습니다."),
    TOSS_PAYMENT_FAILED(HttpStatus.BAD_GATEWAY, "토스 결제 승인에 실패했습니다."),
    TOSS_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "결제 금액이 일치하지 않습니다."),
    TOSS_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "주문 정보가 일치하지 않습니다.");

    // TODO: delivery / dispute 담당자가 각자 도메인 에러코드를 이어서 추가

    private final HttpStatus status;
    private final String message;

    ErrorCode( HttpStatus status, String message ) {
        this.status = status;
        this.message = message;
    }
}
