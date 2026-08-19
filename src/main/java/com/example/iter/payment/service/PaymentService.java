package com.example.iter.payment.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.payment.client.TossApiException;
import com.example.iter.payment.client.TossPaymentClient;
import com.example.iter.payment.config.TossProperties;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.dto.request.PaymentConfirmRequest;
import com.example.iter.payment.dto.response.PaymentConfirmResponse;
import com.example.iter.payment.dto.response.PaymentReadyResponse;
import com.example.iter.payment.dto.toss.TossConfirmApiResponse;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final TossProperties tossProperties;
    private final PaymentFailureRecorder paymentFailureRecorder;

    public PaymentReadyResponse ready( Long rentalId, Long renterId) {
        Rental rental = getPayableRental(rentalId, renterId);

        boolean alreadyPaid = paymentRepository.findByRentalId(rentalId)
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .isPresent();

        if(alreadyPaid){
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        BigDecimal amount = rental.getTotalPrice();
        String orderId = UUID.randomUUID().toString();

        Payment payment = paymentRepository.findByRentalId(rentalId)
                .orElseGet(() -> Payment.builder().rentalId(rentalId).amount(amount).build());
        payment.assignOrder(orderId, amount);

        paymentRepository.save(payment);
        return PaymentReadyResponse.of(rental, orderId, amount, tossProperties.clientKey());
    }

    public PaymentConfirmResponse confirm( Long rentalId, Long renterId, PaymentConfirmRequest request ) {
        Rental rental = getPayableRental(rentalId, renterId);

        Payment payment = paymentRepository.findByRentalId(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_PAYABLE));

        if(payment.getStatus() == PaymentStatus.PAID){
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        if(!Objects.equals(payment.getOrderId(), request.orderId())){
            throw new CustomException(ErrorCode.TOSS_ORDER_MISMATCH);
        }

        if (payment.getAmount().compareTo(request.amount()) != 0) {
            throw new CustomException(ErrorCode.TOSS_AMOUNT_MISMATCH);
        }


        try {
            TossConfirmApiResponse tossResponse =
                    tossPaymentClient.confirm(request.paymentKey(), payment.getOrderId(),
                                              payment.getAmount(), payment.getIdempotencyKey());
            payment.markPaid(tossResponse.paymentKey(),
                             tossResponse.approvedAtAsLocalDateTime());
        } catch (TossApiException e) {
            paymentFailureRecorder.recordFailure(payment.getId());
            throw new CustomException(ErrorCode.TOSS_PAYMENT_FAILED);
        }

        rental.changeStatus(RentalStatus.REQUESTED);
        return PaymentConfirmResponse.of(rental, payment);
    }

    private Rental getPayableRental(Long rentalId, Long renterId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));

        if (!rental.isRenter(renterId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if(rental.getStatus() != RentalStatus.PENDING) {
            throw new CustomException(ErrorCode.RENTAL_NOT_PAYABLE);
        }

        return rental;
    }
}
