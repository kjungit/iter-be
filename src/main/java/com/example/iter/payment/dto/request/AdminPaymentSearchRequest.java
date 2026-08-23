package com.example.iter.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.iter.payment.domain.entity.PaymentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminPaymentSearchRequest(
        @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
        String keyword,

        PaymentStatus status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        @Size(max = 200, message = "커서는 200자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public AdminPaymentSearchRequest {
        size = size == null ? 20 : size;
    }

    @JsonIgnore
    @AssertTrue(message = "조회 시작일은 종료일보다 늦을 수 없습니다.")
    public boolean isDateRangeValid() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
