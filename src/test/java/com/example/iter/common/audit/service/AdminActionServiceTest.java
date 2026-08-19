package com.example.iter.common.audit.service;

import com.example.iter.common.audit.domain.entity.AdminAction;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.domain.repository.AdminActionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminActionServiceTest {

    @Mock
    private AdminActionRepository adminActionRepository;

    @InjectMocks
    private AdminActionService adminActionService;

    @Test
    void 관리자_조치_이력을_전달받은_내용대로_저장한다() {
        adminActionService.record(
                1L,
                AdminActionTargetType.USER,
                2L,
                AdminActionType.SUSPEND_USER,
                "신고 누적"
        );

        ArgumentCaptor<AdminAction> captor = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(captor.capture());

        AdminAction savedAction = captor.getValue();
        assertThat(savedAction.getAdminId()).isEqualTo(1L);
        assertThat(savedAction.getTargetType()).isEqualTo(AdminActionTargetType.USER);
        assertThat(savedAction.getTargetId()).isEqualTo(2L);
        assertThat(savedAction.getAction()).isEqualTo(AdminActionType.SUSPEND_USER);
        assertThat(savedAction.getReason()).isEqualTo("신고 누적");
    }
}
