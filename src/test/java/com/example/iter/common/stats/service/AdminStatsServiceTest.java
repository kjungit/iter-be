package com.example.iter.common.stats.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.stats.dto.response.AdminStatsResponse;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminStatsService adminStatsService;

    @Test
    void 회원_장비_접수된_신고_결제_건수를_각각_집계한다() {
        when(userRepository.count()).thenReturn(120L);
        when(equipmentRepository.count()).thenReturn(45L);
        when(reportRepository.countByStatus(ReportStatus.RECEIVED)).thenReturn(3L);
        when(paymentRepository.count()).thenReturn(300L);

        AdminStatsResponse result = adminStatsService.getStats();

        assertThat(result).isEqualTo(AdminStatsResponse.of(120L, 45L, 3L, 300L));

        verify(userRepository).count();
        verify(equipmentRepository).count();
        verify(reportRepository).countByStatus(ReportStatus.RECEIVED);
        verify(paymentRepository).count();
    }

    @Test
    void 접수_상태가_아닌_신고는_접수된_신고_건수에_포함하지_않는다() {
        when(reportRepository.countByStatus(ReportStatus.RECEIVED)).thenReturn(7L);

        adminStatsService.getStats();

        verify(reportRepository).countByStatus(ReportStatus.RECEIVED);
        verify(reportRepository, never()).countByStatus(ReportStatus.UNDER_REVIEW);
        verify(reportRepository, never()).countByStatus(ReportStatus.RESOLVED);
        verify(reportRepository, never()).countByStatus(ReportStatus.REJECTED);
    }
}
