package com.example.iter.reservation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// 수령/반납 증빙 사진 업로드용 presigned URL 요청 — 장비 이미지(device 도메인)와 달리
// temp->public 승격 단계가 없어 훨씬 단순하다: 발급받은 objectKey가 곧 최종 위치다.
public record EvidenceImagePresignRequest(
        @NotEmpty(message = "이미지는 최소 1장 필요합니다.")
        @Size(max = 4, message = "이미지는 최대 4장까지 업로드할 수 있습니다.")
        List<@Valid Item> files
) {
    public record Item(
            @NotBlank(message = "Content-Type은 필수입니다.")
            @Pattern(
                    regexp = "image/jpeg|image/png|image/webp",
                    message = "JPEG, PNG, WebP 형식만 지원합니다."
            )
            String contentType
    ) {
    }
}
