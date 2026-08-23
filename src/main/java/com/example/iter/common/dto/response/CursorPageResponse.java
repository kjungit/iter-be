package com.example.iter.common.dto.response;

import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;

import java.util.List;
import java.util.function.Function;

// offset(LIMIT/OFFSET) 대신 keyset(cursor) 방식 페이지네이션 응답.
// "이전 페이지에서 마지막으로 받은 항목의 id"를 다음 요청의 cursor로 돌려받아 이어서 조회한다.
// 총 개수/총 페이지가 없는 대신, 몇 번째 페이지를 조회하든 OFFSET 없이 항상 같은 속도로 응답한다.
/**
 * 전체 건수 조회 없이 다음 페이지 존재 여부와 다음 커서를 반환합니다.
 */
public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
        String nextCursor,
        boolean hasNext,
        int size
) {
    // 리포지토리에서 요청한 size보다 1개 더 많이(size + 1) 가져온 뒤 이 메서드에 넘기면,
    // 실제로 다음 페이지가 있는지(hasNext)를 별도 count 쿼리 없이 판단할 수 있다.
    public static <T> CursorPageResponse<T> of(List<T> fetched, int requestedSize, Function<T, Long> cursorExtractor) {
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
        List<T> content = hasNext ? fetched.subList(0, requestedSize) : fetched;
        Long nextCursor = hasNext ? cursorExtractor.apply(content.get(content.size() - 1)) : null;
        return new CursorPageResponse<>(List.copyOf(content), nextCursor, hasNext);
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
