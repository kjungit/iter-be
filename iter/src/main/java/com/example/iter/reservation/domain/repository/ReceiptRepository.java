package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByRentalId(Long rentalId);
}
