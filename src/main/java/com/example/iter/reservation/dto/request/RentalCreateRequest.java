package com.example.iter.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RentalCreateRequest(
        @NotNull(message = "장비 ID는 필수입니다.")
        Long equipmentId,

        @NotNull(message = "대여 시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "대여 종료일은 필수입니다.")
        LocalDate endDate,

        @NotBlank(message = "수령인 이름은 필수입니다.")
        @Size(max = 20, message = "수령인 이름은 20자 이하여야 합니다.")
        String receiverName,

        @NotBlank(message = "수령인 연락처는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String receiverPhone,

        @NotBlank(message = "우편번호는 필수입니다.")
        @Size(max = 10, message = "우편번호는 10자 이하여야 합니다.")
        String zipcode,

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
        String address,

        @Size(max = 200, message = "상세주소는 200자 이하여야 합니다.")
        String detailAddress,

        String requestMessage,

        Boolean useDefaultAddress
) {
}
