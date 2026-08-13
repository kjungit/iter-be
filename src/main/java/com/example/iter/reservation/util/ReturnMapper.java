package com.example.iter.reservation.util;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.reservation.domain.entity.Receipt;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.ReturnReceipt;
import com.example.iter.reservation.dto.response.ConditionEvidenceResponse;
import com.example.iter.reservation.dto.response.ReturnComparisonResponse;
import com.example.iter.reservation.dto.response.ReturnConfirmationResponse;
import com.example.iter.reservation.dto.response.ReturnTargetResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReturnMapper {

    // 반납 확인 대상 목록 응답으로 변환합니다.
    public ReturnTargetResponse toTarget(
            Rental rental,
            User renter,
            String thumbnailUrl,
            ReturnReceipt returnReceipt
    ) {
        return new ReturnTargetResponse(
                rental.getId(),
                rental.getProductNameSnapshot(),
                thumbnailUrl,
                toUserSummary(renter),
                rental.getEndDate(),
                returnReceipt.getReturnDate()
        );
    }

    // 수령·반납 증빙 비교 응답으로 변환합니다.
    public ReturnComparisonResponse toComparison(
            Rental rental,
            User renter,
            Receipt receipt,
            List<String> receiptImageUrls,
            ReturnReceipt returnReceipt,
            List<String> returnImageUrls
    ) {
        return new ReturnComparisonResponse(
                rental.getId(),
                rental.getProductNameSnapshot(),
                toUserSummary(renter),
                rental.getStartDate(),
                rental.getEndDate(),
                returnReceipt.getReturnDate(),
                toEvidence(receipt, receiptImageUrls),
                toEvidence(returnReceipt, returnImageUrls)
        );
    }

    // 반납 최종 확인 응답으로 변환합니다.
    public ReturnConfirmationResponse toConfirmation(
            Rental rental,
            Long disputeId
    ) {
        return new ReturnConfirmationResponse(
                rental.getId(),
                rental.getStatus(),
                disputeId
        );
    }

    // ============================================================

    private ConditionEvidenceResponse toEvidence(
            Receipt receipt,
            List<String> imageUrls
    ) {
        return new ConditionEvidenceResponse(
                receipt.getProductCondition(),
                receipt.getConditionDetail(),
                imageUrls,
                receipt.getReceivedAt()
        );
    }

    private ConditionEvidenceResponse toEvidence(
            ReturnReceipt returnReceipt,
            List<String> imageUrls
    ) {
        return new ConditionEvidenceResponse(
                returnReceipt.getProductCondition(),
                returnReceipt.getConditionDetail(),
                imageUrls,
                returnReceipt.getCreatedAt()
        );
    }

    private UserSummaryResponse toUserSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getNickName()
        );
    }

}
