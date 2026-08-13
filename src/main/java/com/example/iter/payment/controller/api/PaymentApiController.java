package com.example.iter.payment.controller.api;

import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.payment.dto.response.PaymentResponse;
import com.example.iter.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "결제(포인트) API")
@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @Operation(summary = "결제(포인트)", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/{rentalId}/payment")
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId) {
        PaymentResponse response = paymentService.payRental(rentalId, principal.getUser().getId());
        return ResponseEntity.ok(response);
    }
}
