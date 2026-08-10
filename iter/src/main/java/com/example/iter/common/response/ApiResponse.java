package com.example.iter.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

// 프로젝트 전역에서 사용하는 공통 응답 포맷
// (기획서 DoD "일관된 성공/에러 응답 구조 적용" 항목 대응)
//
// - 성공: { "success": true, "data": ..., "message": null }
// - 실패: { "success": false, "data": null, "message": "..." }  (message는 GlobalExceptionHandler에서 채움)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
