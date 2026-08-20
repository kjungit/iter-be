package com.example.iter.payment.controller.api.spec;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.payment.dto.request.PaymentHistorySearchRequest;
import com.example.iter.payment.dto.response.PaymentHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payment History", description = "마이페이지 결제 내역 API")
@SecurityRequirement(name = "JWT")
public interface PaymentHistoryApiSpec {

    @Operation(summary = "내 결제 내역 조회", description = "본인이 결제한 대여 건의 현재 결제 상태를 조회합니다.")
    ResponseEntity<PageResponse<PaymentHistoryResponse>> getMyPaymentHistory(
            @Parameter(hidden = true) CustomUserDetails principal,
            @ParameterObject PaymentHistorySearchRequest request
    );
}
