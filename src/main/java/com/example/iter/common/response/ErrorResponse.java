package com.example.iter.common.response;

import java.util.List;
import java.util.Map;

// code는 ErrorCode.name() (안정적인 식별자, 프론트가 번역 키로 매핑해서 쓴다).
// message는 백엔드 개발자/로그용 한국어 문구일 뿐 프론트가 그대로 화면에 노출하는 용도가 아니다.
// errors는 @Valid 검증 실패처럼 필드 단위로 여러 건이 발생하는 경우에만 채워진다 (그 외엔 빈 리스트).
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> errors
) {
    public static ErrorResponse from(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }

    public static ErrorResponse validation(String code, String message, List<FieldError> errors) {
        return new ErrorResponse(code, message, errors);
    }

    // field: 검증에 실패한 요청 필드명 (예: "size")
    // constraint: 위반한 제약조건 애노테이션의 simple name (예: "Max", "NotBlank", "Size")
    // params: 그 제약조건의 속성값 (예: Max면 {"value": 100}) — message/groups/payload는 제외.
    //         프론트가 이 값으로 자체 번역 문구를 조립한다 (예: "최대 {value}까지 가능합니다").
    public record FieldError(String field, String constraint, Map<String, Object> params) {
    }
}
