package com.example.iter.common.exception;

import com.example.iter.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// API 명세의 { "code": "...", "message": "..." } 오류 형식을 적용한다.
// (기획서 DoD "예외 상황(존재하지 않는 id, 소유권 없는 접근 등) 핸들링 적용" 대응)
//
// code/message는 그대로 백엔드 한국어 문구다(로그/개발자용, 프론트 표시용 아님) — 실제 번역은 프론트가
// code(ErrorCode.name())를 키로 자체 사전에서 담당한다. @Valid 검증 실패는 한 줄 message로 뭉개지 않고
// errors(필드별 constraint + params) 구조로도 같이 내려줘서, 프론트가 "Size, {max: 100}" 같은 걸로
// 자체 문구를 조립할 수 있게 한다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bean Validation 애노테이션 속성 중 프론트에 의미 없는 것들 — params에서 제외
    private static final Set<String> EXCLUDED_CONSTRAINT_ATTRIBUTES = Set.of("message", "groups", "payload");

    // 도메인에서 의도적으로 던진 비즈니스 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException: {} - {}", errorCode, e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode.name(), e.getMessage()));
    }

    // @Valid 검증 실패 (요청 DTO의 @NotNull, @NotBlank 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        log.warn("Validation 실패: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validation(ErrorCode.VALIDATION_ERROR.name(), message, errors));
    }

    // @RequestParam, @PathVariable 등에 붙은 제약조건 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolation: {}", e.getMessage());
        List<ErrorResponse.FieldError> errors = e.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validation(
                        ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getMessage(), errors));
    }

    // MethodArgumentNotValidException의 FieldError는 @Valid/Hibernate Validator를 거쳤으면 내부적으로
    // ConstraintViolation을 감싸고 있다 — unwrap으로 그걸 꺼내서 constraint 애노테이션 이름과 속성을 얻는다.
    // 이 프로젝트엔 커스텀 Spring Validator가 없어서 항상 성공하지만, 혹시 Bean Validation을 거치지 않은
    // FieldError(예: errors.rejectValue 직접 호출)가 생기면 unwrap이 실패하므로 대비해서 폴백한다.
    private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
        try {
            ConstraintViolation<?> violation = fieldError.unwrap(ConstraintViolation.class);
            return toFieldError(fieldError.getField(), violation);
        } catch (IllegalArgumentException ex) {
            return new ErrorResponse.FieldError(fieldError.getField(), fieldError.getCode(), Map.of());
        }
    }

    private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        return toFieldError(field, violation);
    }

    private ErrorResponse.FieldError toFieldError(String field, ConstraintViolation<?> violation) {
        ConstraintDescriptor<?> descriptor = violation.getConstraintDescriptor();
        String constraint = descriptor.getAnnotation().annotationType().getSimpleName();
        Map<String, Object> params = descriptor.getAttributes().entrySet().stream()
                .filter(entry -> !EXCLUDED_CONSTRAINT_ATTRIBUTES.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ErrorResponse.FieldError(field, constraint, params);
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("요청 본문을 읽을 수 없음: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(
                        ErrorCode.VALIDATION_ERROR.name(),
                        ErrorCode.VALIDATION_ERROR.getMessage()
                ));
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

    // 인가 실패 (소유권 없음, 권한 부족 등 — @PreAuthorize에서 발생)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDenied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.from(ErrorCode.FORBIDDEN.name(), ErrorCode.FORBIDDEN.getMessage()));
    }

    // 같은 행을 동시에 수정해서 발생한 낙관적 락(@Version) 충돌 — 서버 오류가 아니라
    // "먼저 처리된 다른 요청과 경합했다"는 신호이므로 500이 아니라 409로 내려준다.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException e) {
        log.warn("낙관적 락 충돌: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.CONCURRENT_MODIFICATION.getStatus())
                .body(ErrorResponse.from(
                        ErrorCode.CONCURRENT_MODIFICATION.name(), ErrorCode.CONCURRENT_MODIFICATION.getMessage()));
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
