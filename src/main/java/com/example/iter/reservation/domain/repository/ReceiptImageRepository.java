package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.ReceiptImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptImageRepository extends JpaRepository<ReceiptImage, Long> {
    // 수령 증빙 이미지를 등록 순서대로 조회합니다.
    List<ReceiptImage> findByReceipt_IdOrderBySortOrderAscIdAsc(Long receiptId);
}
