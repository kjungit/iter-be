package com.example.iter.payment.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.dto.response.PaymentResponse;
import com.example.iter.payment.exception.PointInsufficientException;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public PaymentResponse payRental(Long rentalId, Long renterId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));

        if (!rental.isRenter(renterId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (rental.getStatus() != RentalStatus.PENDING) {
            throw new CustomException(ErrorCode.RENTAL_NOT_PAYABLE);
        }

        boolean alreadyPaid = paymentRepository.findByRentalId(rentalId)
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .isPresent();
        if (alreadyPaid) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        BigDecimal amount = rental.getTotalPrice();
        int updatedRows = userRepository.deductPointBalance(renterId, amount);
        if (updatedRows == 0) {
            BigDecimal currentBalance = userRepository.findById(renterId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                    .getPointBalance();
            throw new PointInsufficientException(currentBalance, amount);
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .rentalId(rentalId)
                .amount(amount)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build());

        rental.changeStatus(RentalStatus.REQUESTED);

        BigDecimal pointBalance = userRepository.findById(renterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getPointBalance();

        return PaymentResponse.of(rental, payment, pointBalance);
    }
}
