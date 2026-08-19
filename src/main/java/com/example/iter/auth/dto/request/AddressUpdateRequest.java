package com.example.iter.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @NotBlank(message = "수령인 이름은 필수입니다.")
        @Size(max = 20, message = "수령인 이름은 20자 이하여야 합니다.")
        String recipientName,

        @NotBlank(message = "수령인 연락처는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        String recipientPhone,

        @NotBlank(message = "우편번호는 필수입니다.")
        @Size(max = 10, message = "우편번호는 10자 이하여야 합니다.")
        String zipcode,

        @NotBlank(message = "기본 주소는 필수입니다.")
        @Size(max = 200, message = "기본 주소는 200자 이하여야 합니다.")
        String address,

        @NotBlank(message = "상세 주소는 필수입니다.")
        @Size(max = 200, message = "상세 주소는 200자 이하여야 합니다.")
        String detailAddress
) {
}
