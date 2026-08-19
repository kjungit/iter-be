package com.example.iter.payment.domain.repository;

import com.example.iter.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRentalId(Long rentalId);
    Optional<Payment> findByOrderId(String orderId);
}
