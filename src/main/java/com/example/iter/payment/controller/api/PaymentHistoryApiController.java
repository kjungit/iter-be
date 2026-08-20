package com.example.iter.payment.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.payment.dto.request.PaymentHistorySearchRequest;
import com.example.iter.payment.dto.response.PaymentHistoryResponse;
import com.example.iter.payment.service.PaymentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment History", description = "마이페이지 결제 내역 API")
@SecurityRequirement(name = "JWT")
@RestController
@RequestMapping("/api/v1/users/me/payments")
@RequiredArgsConstructor
public class PaymentHistoryApiController {

    private final PaymentHistoryService paymentHistoryService;

    @Operation(summary = "내 결제 내역 조회", description = "본인이 결제한 대여 건의 현재 결제 상태를 조회합니다.")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentHistoryResponse>> getMyPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute PaymentHistorySearchRequest request
    ) {
        return ResponseEntity.ok(paymentHistoryService.getMyPaymentHistory(
                principal.getUser().getId(),
                request
        ));
    }
}
