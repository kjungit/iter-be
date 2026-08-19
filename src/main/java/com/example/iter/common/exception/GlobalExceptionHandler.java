package com.example.iter.common.exception;

import com.example.iter.common.response.ErrorResponse;
import com.example.iter.payment.dto.response.PointInsufficientErrorResponse;
import com.example.iter.payment.exception.PointInsufficientException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// API 명세의 { "code": "...", "message": "..." } 오류 형식을 적용한다.
// (기획서 DoD "예외 상황(존재하지 않는 id, 소유권 없는 접근 등) 핸들링 적용" 대응)
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 도메인에서 의도적으로 던진 비즈니스 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException: {} - {}", errorCode, e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode.name(), errorCode.getMessage()));
    }

    // @Valid 검증 실패 (요청 DTO의 @NotNull, @NotBlank 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        log.warn("Validation 실패: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(ErrorCode.VALIDATION_ERROR.name(), message));
    }

    // @RequestParam, @PathVariable 등에 붙은 제약조건 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolation: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getMessage()));
    }

    // @ModelAttribute 바인딩 실패 또는 enum 등 요청 파라미터 타입 변환 실패
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleRequestBindingException(Exception e) {
        log.warn("요청 파라미터 바인딩 실패: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(
                        ErrorCode.VALIDATION_ERROR.name(),
                        ErrorCode.VALIDATION_ERROR.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e
    ) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());
        return ResponseEntity
                .status(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
                .body(ErrorResponse.from(
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage()
                ));
    }

    // 포인트 부족 (B 담당) — message 외에 pointBalance/requiredAmount를 같이 내려줘야 해서 별도 예외/응답 타입으로 분리
    @ExceptionHandler(PointInsufficientException.class)
    public ResponseEntity<PointInsufficientErrorResponse> handlePointInsufficient(PointInsufficientException e) {
        log.warn("PointInsufficient: balance={}, required={}", e.getPointBalance(), e.getRequiredAmount());
        return ResponseEntity
                .status(ErrorCode.POINT_INSUFFICIENT.getStatus())
                .body(PointInsufficientErrorResponse.of(e.getMessage(), e.getPointBalance(), e.getRequiredAmount()));
    }

    // 인가 실패 (소유권 없음, 권한 부족 등 — @PreAuthorize에서 발생)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDenied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.from(ErrorCode.FORBIDDEN.name(), ErrorCode.FORBIDDEN.getMessage()));
    }

    // 그 외 예상하지 못한 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상하지 못한 예외 발생", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(ErrorCode.INTERNAL_SERVER_ERROR.name(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
