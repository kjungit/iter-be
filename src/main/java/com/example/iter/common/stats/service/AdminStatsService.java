package com.example.iter.common.stats.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.stats.dto.response.AdminStatsResponse;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final ReportRepository reportRepository;
    private final PaymentRepository paymentRepository;

    // 관리자 대시보드 상단 카드에 필요한 회원·장비·신고·결제 건수를 각각 COUNT 쿼리로 조회합니다.
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.of(
                userRepository.count(),
                equipmentRepository.count(),
                reportRepository.countByStatus(ReportStatus.RECEIVED),
                paymentRepository.count()
        );
    }
}
