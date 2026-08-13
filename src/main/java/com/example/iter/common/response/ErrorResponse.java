package com.example.iter.common.response;

public record ErrorResponse(
        String code,
        String message
) {
    public static ErrorResponse from(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
