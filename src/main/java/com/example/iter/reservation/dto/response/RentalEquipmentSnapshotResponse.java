package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.Rental;

import java.math.BigDecimal;

public record RentalEquipmentSnapshotResponse(
        Long equipmentId,
        String equipmentName,
        String category,
        BigDecimal dailyPrice,
        String thumbnailUrl
) {
    // 현재 Equipment가 아니라 예약 시점에 Rental에 복사해둔 스냅샷을 써야 이후 장비 정보가
    // 바뀌어도 과거 예약 조회 결과가 그대로 유지된다.
    // TODO: 장비 썸네일 URL — 스냅샷에 아직 없음, EquipmentImage 연동 필요 (A 담당 영역), 우선 null
    public static RentalEquipmentSnapshotResponse from(Rental rental) {
        return new RentalEquipmentSnapshotResponse(
                rental.getEquipmentId(),
                rental.getProductNameSnapshot(),
                rental.getCategorySnapshot(),
                rental.getDailyPriceSnapshot(),
                null
        );
    }
}
