package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.ProductConditionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionEvidenceResponseTest {

    @Test
    void 이미지_URL이_null이면_빈_목록으로_변환한다() {
        var response = new ConditionEvidenceResponse(
                ProductConditionType.NORMAL,
                "정상",
                null,
                LocalDateTime.of(2026, 8, 1, 14, 30)
        );

        assertThat(response.imageUrls()).isEmpty();
    }

    @Test
    void 이미지_URL은_방어적_복사되어_원본_목록_변경의_영향을_받지_않는다() {
        List<String> source = new ArrayList<>(List.of("first.jpg"));
        var response = new ConditionEvidenceResponse(
                ProductConditionType.NORMAL,
                "정상",
                source,
                LocalDateTime.of(2026, 8, 1, 14, 30)
        );

        source.add("second.jpg");

        assertThat(response.imageUrls()).containsExactly("first.jpg");
    }

    @Test
    void 응답의_이미지_URL_목록은_수정할_수_없다() {
        var response = new ConditionEvidenceResponse(
                ProductConditionType.NORMAL,
                "정상",
                List.of("first.jpg"),
                LocalDateTime.of(2026, 8, 1, 14, 30)
        );

        assertThatThrownBy(() -> response.imageUrls().add("second.jpg"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
