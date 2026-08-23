package com.example.iter.payment.service;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.markFailed();
            log.warn("결제 승인 실패 상태 기록: paymentId={}, rentalId={}, status={}",
                    payment.getId(), payment.getRentalId(), payment.getStatus());
        });
    }
}
