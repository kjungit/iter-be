package com.example.iter.common.pagination;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorPaginationTest {

    @Test
    void 커서를_인코딩하고_원래_키로_복원한다() {
        CursorKey expected = new CursorKey(
                LocalDateTime.of(2026, 8, 23, 12, 34, 56, 123_456_000),
                100L
        );

        CursorKey decoded = CursorCodec.decode(CursorCodec.encode(expected));

        assertThat(decoded).isEqualTo(expected);
    }

    @Test
    void 커서를_생략하면_첫_페이지로_판단한다() {
        assertThat(CursorCodec.decode(null)).isNull();
        assertThat(CursorCodec.decode("   ")).isNull();
    }

    @Test
    void 형식이_잘못된_커서는_VALIDATION_ERROR를_발생시킨다() {
        assertThatThrownBy(() -> CursorCodec.decode("invalid-cursor"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 요청_크기보다_한_건_더_조회해_다음_커서를_생성한다() {
        LocalDateTime latest = LocalDateTime.of(2026, 8, 23, 12, 0);
        List<TestRow> fetched = List.of(
                new TestRow(3L, latest),
                new TestRow(2L, latest.minusMinutes(1)),
                new TestRow(1L, latest.minusMinutes(2))
        );

        CursorPageResponse<Long> response = CursorPageResponse.from(
                fetched,
                2,
                TestRow::id,
                row -> new CursorKey(row.createdAt(), row.id())
        );

        assertThat(response.content()).containsExactly(3L, 2L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.size()).isEqualTo(2);
        assertThat(CursorCodec.decode(response.nextCursor()))
                .isEqualTo(new CursorKey(latest.minusMinutes(1), 2L));
    }

    @Test
    void 마지막_페이지에는_다음_커서를_반환하지_않는다() {
        TestRow row = new TestRow(1L, LocalDateTime.of(2026, 8, 23, 12, 0));

        CursorPageResponse<Long> response = CursorPageResponse.from(
                List.of(row),
                2,
                TestRow::id,
                item -> new CursorKey(item.createdAt(), item.id())
        );

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    private record TestRow(Long id, LocalDateTime createdAt) {
    }
}
