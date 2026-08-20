package com.example.iter.payment.controller.api;

import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.payment.dto.request.PaymentConfirmRequest;
import com.example.iter.payment.dto.response.PaymentConfirmResponse;
import com.example.iter.payment.dto.response.PaymentReadyResponse;
import com.example.iter.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "토스페이먼츠 결제 API")
@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @Operation(
            summary = "결제 준비 (orderId 발급)", security = @SecurityRequirement(
            name = "JWT"
    )
    )
    @PostMapping("/{rentalId}/payment/ready")
    public ResponseEntity<PaymentReadyResponse> ready(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId ) {
        PaymentReadyResponse response = paymentService.ready(rentalId, principal.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/payment/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId,
            @Valid @RequestBody PaymentConfirmRequest request
                                                         ) {
        PaymentConfirmResponse response = paymentService.confirm(rentalId, principal.getUser().getId(), request);
        return ResponseEntity.ok(response);
    }
}
