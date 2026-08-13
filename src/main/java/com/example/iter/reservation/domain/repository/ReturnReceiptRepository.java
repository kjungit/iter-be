package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.ReturnReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnReceiptRepository extends JpaRepository<ReturnReceipt, Long> {
    Optional<ReturnReceipt> findByRentalId(Long rentalId);

    // 여러 거래의 반납 증빙을 한 번에 조회합니다.
    List<ReturnReceipt> findAllByRental_IdIn(Collection<Long> rentalIds);
}
