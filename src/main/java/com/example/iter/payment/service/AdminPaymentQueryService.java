package com.example.iter.payment.service;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.payment.domain.repository.AdminPaymentQueryRepository;
import com.example.iter.payment.dto.request.AdminPaymentSearchRequest;
import com.example.iter.payment.dto.response.AdminPaymentDetailResponse;
import com.example.iter.payment.dto.response.AdminPaymentSummaryResponse;
import com.example.iter.payment.service.model.AdminPaymentDetailRow;
import com.example.iter.payment.service.model.AdminPaymentSummaryRow;
import com.example.iter.payment.util.AdminPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminPaymentQueryService {

    private final AdminPaymentQueryRepository adminPaymentQueryRepository;
    private final AdminPaymentMapper adminPaymentMapper;

    // 관리자가 검색 조건으로 전체 결제 기록을 페이지 단위로 조회합니다.
    @Transactional(readOnly = true)
    public PageResponse<AdminPaymentSummaryResponse> getPayments(AdminPaymentSearchRequest request) {
        PageRequest pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<AdminPaymentSummaryRow> paymentPage = adminPaymentQueryRepository.searchForAdmin(
                normalize(request.keyword()),
                request.status(),
                toStartOfDay(request.fromDate()),
                toNextStartOfDay(request.toDate()),
                pageable
        );

        return PageResponse.from(paymentPage.map(adminPaymentMapper::toSummary));
    }

    // 관리자가 특정 결제와 연결된 대여 상세 정보를 조회합니다.
    @Transactional(readOnly = true)
    public AdminPaymentDetailResponse getPayment(Long paymentId) {
        AdminPaymentDetailRow payment = adminPaymentQueryRepository.findDetailById(paymentId).orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        return adminPaymentMapper.toDetail(payment);
    }

    // 검색 문자열의 앞뒤 공백을 제거하고 빈 문자열은 조회 조건에서 제외합니다.
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // 조회 시작일을 해당 날짜의 시작 시각으로 변환합니다.
    private LocalDateTime toStartOfDay(java.time.LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    // 조회 종료일 전체를 포함하기 위해 종료일 다음 날의 시작 시각으로 변환합니다.
    private LocalDateTime toNextStartOfDay(java.time.LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }
}
