package com.example.iter.payment.controller.admin;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.payment.controller.admin.spec.AdminPaymentApiSpec;
import com.example.iter.payment.dto.request.AdminPaymentSearchRequest;
import com.example.iter.payment.dto.response.AdminPaymentDetailResponse;
import com.example.iter.payment.dto.response.AdminPaymentSummaryResponse;
import com.example.iter.payment.service.AdminPaymentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentApiController implements AdminPaymentApiSpec {

    private final AdminPaymentQueryService adminPaymentQueryService;

    // 관리자가 검색 조건과 페이지 정보로 결제 목록을 조회합니다.
    @Override
    @GetMapping
    public ResponseEntity<CursorPageResponse<AdminPaymentSummaryResponse>> getPayments(@ModelAttribute AdminPaymentSearchRequest request) {
        return ResponseEntity.ok(adminPaymentQueryService.getPayments(request));
    }

    // 관리자가 특정 결제와 연결된 대여 정보를 상세 조회합니다.
    @Override
    @GetMapping("/{paymentId}")
    public ResponseEntity<AdminPaymentDetailResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(adminPaymentQueryService.getPayment(paymentId));
    }
}
