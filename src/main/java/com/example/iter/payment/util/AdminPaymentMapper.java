package com.example.iter.payment.util;

import com.example.iter.payment.dto.response.AdminPaymentDetailResponse;
import com.example.iter.payment.dto.response.AdminPaymentRentalResponse;
import com.example.iter.payment.dto.response.AdminPaymentSummaryResponse;
import com.example.iter.payment.service.model.AdminPaymentDetailRow;
import com.example.iter.payment.service.model.AdminPaymentSummaryRow;
import org.springframework.stereotype.Component;

@Component
public class AdminPaymentMapper {

    // 조회 결과를 관리자 결제 목록 응답으로 변환합니다.
    public AdminPaymentSummaryResponse toSummary(AdminPaymentSummaryRow row) {
        return new AdminPaymentSummaryResponse(
                row.paymentId(),
                row.rentalId(),
                row.orderId(),
                row.renterId(),
                row.renterEmail(),
                row.renterName(),
                row.renterNickname(),
                row.equipmentName(),
                row.amount(),
                row.paymentStatus(),
                row.rentalStatus(),
                row.paidAt(),
                row.refundedAt(),
                row.createdAt()
        );
    }

    // 상세 조회 결과를 결제 요약과 예약 당시 대여 정보가 포함된 응답으로 변환합니다.
    public AdminPaymentDetailResponse toDetail(AdminPaymentDetailRow row) {
        AdminPaymentSummaryResponse summary = new AdminPaymentSummaryResponse(
                row.paymentId(),
                row.rentalId(),
                row.orderId(),
                row.renterId(),
                row.renterEmail(),
                row.renterName(),
                row.renterNickname(),
                row.equipmentName(),
                row.amount(),
                row.paymentStatus(),
                row.rentalStatus(),
                row.paidAt(),
                row.refundedAt(),
                row.createdAt()
        );

        AdminPaymentRentalResponse rentalResponse = new AdminPaymentRentalResponse(
                row.equipmentId(),
                row.equipmentName(),
                row.category(),
                row.dailyPrice(),
                row.startDate(),
                row.endDate(),
                row.rentalDays(),
                row.totalPrice(),
                row.rentalStatus()
        );

        return new AdminPaymentDetailResponse(
                summary,
                rentalResponse,
                row.updatedAt()
        );
    }
}
