package com.example.iter.dispute.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportTargetValidator {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalRepository rentalRepository;

    // 신고 대상 유형에 맞게 대상 존재 여부와 신고 권한을 검증합니다.
    public void validate(ReportTargetType targetType, Long targetId, Long reporterId) {
        switch (targetType) {
            case USER -> validateUserTarget(targetId, reporterId);
            case EQUIPMENT -> validateEquipmentTarget(targetId, reporterId);
            case RENTAL -> validateRentalTarget(targetId, reporterId);
        }
    }

    // 자기 자신과 탈퇴한 회원을 신고하지 못하도록 검증합니다.
    private void validateUserTarget(Long targetId, Long reporterId) {
        if (targetId.equals(reporterId)) {
            throw new CustomException(ErrorCode.REPORT_SELF_TARGET_NOT_ALLOWED);
        }

        User targetUser = userRepository.findById(targetId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (targetUser.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    // 본인 소유 장비와 삭제된 장비를 신고하지 못하도록 검증합니다.
    private void validateEquipmentTarget(Long targetId, Long reporterId) {
        Equipment equipment = equipmentRepository.findById(targetId).orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (equipment.getStatus() == EquipmentStatus.DELETED) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        if (equipment.isOwnedBy(reporterId)) {
            throw new CustomException(ErrorCode.REPORT_SELF_TARGET_NOT_ALLOWED);
        }
    }

    // 거래의 대여자 또는 장비 등록자만 해당 거래를 신고할 수 있도록 검증합니다.
    private void validateRentalTarget(Long targetId, Long reporterId) {
        Rental rental = rentalRepository.findById(targetId).orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));

        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId()).orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!rental.isRenter(reporterId) && !equipment.isOwnedBy(reporterId)) {
            throw new CustomException(ErrorCode.RENTAL_NOT_PARTY);
        }
    }
}
