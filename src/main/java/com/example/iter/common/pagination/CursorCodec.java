package com.example.iter.common.pagination;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 커서 내부 값을 URL-safe Base64 문자열로 변환합니다.
 * 커서는 페이지 이동 위치일 뿐 권한 검증이나 보안 토큰으로 사용하지 않습니다.
 */
public final class CursorCodec {

    private static final String DELIMITER = "|";

    private CursorCodec() {
    }

    public static String encode(CursorKey cursorKey) {
        String value = cursorKey.createdAt() + DELIMITER + cursorKey.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorKey decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor.trim()),
                    StandardCharsets.UTF_8
            );
            String[] values = decoded.split("\\|", -1);
            if (values.length != 2) {
                throw invalidCursor();
            }

            LocalDateTime createdAt = LocalDateTime.parse(values[0]);
            long id = Long.parseLong(values[1]);
            if (id <= 0) {
                throw invalidCursor();
            }

            return new CursorKey(createdAt, id);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private static CustomException invalidCursor() {
        return new CustomException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 커서입니다.");
    }
}
