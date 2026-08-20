package com.example.iter.payment.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.payment.controller.api.spec.PaymentHistoryApiSpec;
import com.example.iter.payment.dto.request.PaymentHistorySearchRequest;
import com.example.iter.payment.dto.response.PaymentHistoryResponse;
import com.example.iter.payment.service.PaymentHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/payments")
@RequiredArgsConstructor
public class PaymentHistoryApiController implements PaymentHistoryApiSpec {

    private final PaymentHistoryService paymentHistoryService;

    @Override
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
