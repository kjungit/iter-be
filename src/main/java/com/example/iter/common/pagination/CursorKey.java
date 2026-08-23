package com.example.iter.common.pagination;

import java.time.LocalDateTime;

/**
 * createdAt 내림차순, id 내림차순 keyset 조회에 사용하는 내부 커서 값입니다.
 */
public record CursorKey(
        LocalDateTime createdAt,
        Long id
) {
}
