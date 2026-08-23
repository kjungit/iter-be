package com.example.iter.common.dto.response;

import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;

import java.util.List;
import java.util.function.Function;

/**
 * 전체 건수 조회 없이 다음 페이지 존재 여부와 다음 커서를 반환합니다.
 */
public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext,
        int size
) {
    public CursorPageResponse {
        content = List.copyOf(content);
    }

    public static <S, T> CursorPageResponse<T> from(
            List<S> fetched,
            int requestedSize,
            Function<S, T> mapper,
            Function<S, CursorKey> cursorKeyExtractor
    ) {
        boolean hasNext = fetched.size() > requestedSize;
        List<S> pageContent = hasNext
                ? fetched.subList(0, requestedSize)
                : fetched;
        List<T> content = pageContent.stream().map(mapper).toList();

        String nextCursor = hasNext && !pageContent.isEmpty()
                ? CursorCodec.encode(cursorKeyExtractor.apply(pageContent.getLast()))
                : null;

        return new CursorPageResponse<>(content, nextCursor, hasNext, requestedSize);
    }
}
