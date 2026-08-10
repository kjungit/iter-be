package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.ReturnReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnReceiptRepository extends JpaRepository<ReturnReceipt, Long> {
    Optional<ReturnReceipt> findByRentalId(Long rentalId);
}
