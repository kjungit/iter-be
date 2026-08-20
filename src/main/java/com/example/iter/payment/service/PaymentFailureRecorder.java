package com.example.iter.payment.service;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(Payment::markFailed);
    }
}