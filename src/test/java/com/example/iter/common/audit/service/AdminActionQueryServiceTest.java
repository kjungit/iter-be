package com.example.iter.common.audit.service;

import com.example.iter.common.audit.domain.entity.AdminAction;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.domain.repository.AdminActionRepository;
import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.audit.util.AdminActionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActionQueryServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long EQUIPMENT_ID = 10L;

    @Mock
    private AdminActionRepository adminActionRepository;

    @Mock
    private AdminActionMapper adminActionMapper;

    @InjectMocks
    private AdminActionQueryService adminActionQueryService;

    @Test
    void 관리자_처리_이력을_검색하고_커서로_조회한다() {
        AdminAction firstAction = adminAction(
                101L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "신고 누적으로 관리자 차단"
        );
        AdminAction secondAction = adminAction(
                100L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "반복적인 정책 위반"
        );
        AdminActionResponse firstResponse = response(
                101L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "신고 누적으로 관리자 차단",
                LocalDateTime.of(2026, 8, 2, 11, 0)
        );
        AdminActionResponse secondResponse = response(
                100L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "반복적인 정책 위반",
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        AdminActionSearchRequest request = new AdminActionSearchRequest(
                AdminActionTargetType.EQUIPMENT,
                EQUIPMENT_ID,
                AdminActionType.SUSPEND_EQUIPMENT,
                null,
                10
        );

        when(adminActionRepository.searchForAdminByCursor(
                eq(AdminActionTargetType.EQUIPMENT),
                eq(EQUIPMENT_ID),
                eq(AdminActionType.SUSPEND_EQUIPMENT),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of(firstAction, secondAction));
        when(adminActionMapper.toResponse(firstAction))
                .thenReturn(firstResponse);
        when(adminActionMapper.toResponse(secondAction))
                .thenReturn(secondResponse);

        var result = adminActionQueryService.getAdminActions(request);

        assertThat(result.content()).containsExactly(
                firstResponse,
                secondResponse
        );
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionRepository).searchForAdminByCursor(
                eq(AdminActionTargetType.EQUIPMENT),
                eq(EQUIPMENT_ID),
                eq(AdminActionType.SUSPEND_EQUIPMENT),
                isNull(),
                isNull(),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isZero();
        assertThat(capturedPageable.getPageSize()).isEqualTo(11);

        verify(adminActionMapper).toResponse(firstAction);
        verify(adminActionMapper).toResponse(secondAction);
    }

    @Test
    void 검색_조건과_커서가_없으면_null을_전달하고_기본_크기로_조회한다() {
        AdminAction action = adminAction(
                100L,
                AdminActionType.RESTORE_EQUIPMENT,
                "차단 사유 해소"
        );
        AdminActionResponse mappedResponse = response(
                100L,
                AdminActionType.RESTORE_EQUIPMENT,
                "차단 사유 해소",
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        AdminActionSearchRequest request = new AdminActionSearchRequest(
                null,
                null,
                null,
                null,
                null
        );

        when(adminActionRepository.searchForAdminByCursor(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of(action));
        when(adminActionMapper.toResponse(action))
                .thenReturn(mappedResponse);

        var result = adminActionQueryService.getAdminActions(request);

        assertThat(result.content()).containsExactly(mappedResponse);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionRepository).searchForAdminByCursor(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isZero();
        assertThat(capturedPageable.getPageSize()).isEqualTo(21);

        verify(adminActionMapper).toResponse(action);
    }

    @Test
    void 처리_이력_검색_결과가_비어있으면_매퍼를_호출하지_않는다() {
        AdminActionSearchRequest request = new AdminActionSearchRequest(
                AdminActionTargetType.USER,
                20L,
                AdminActionType.SUSPEND_USER,
                null,
                5
        );

        when(adminActionRepository.searchForAdminByCursor(
                eq(AdminActionTargetType.USER),
                eq(20L),
                eq(AdminActionType.SUSPEND_USER),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of());

        var result = adminActionQueryService.getAdminActions(request);

        assertThat(result.content()).isEmpty();
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.hasNext()).isFalse();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionRepository).searchForAdminByCursor(
                eq(AdminActionTargetType.USER),
                eq(20L),
                eq(AdminActionType.SUSPEND_USER),
                isNull(),
                isNull(),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(6);
        verifyNoInteractions(adminActionMapper);
    }

    private AdminAction adminAction(
            Long actionId,
            AdminActionType actionType,
            String reason
    ) {
        return AdminAction.builder()
                .id(actionId)
                .adminId(ADMIN_ID)
                .targetType(AdminActionTargetType.EQUIPMENT)
                .targetId(EQUIPMENT_ID)
                .action(actionType)
                .reason(reason)
                .build();
    }

    private AdminActionResponse response(
            Long actionId,
            AdminActionType actionType,
            String reason,
            LocalDateTime createdAt
    ) {
        return new AdminActionResponse(
                actionId,
                ADMIN_ID,
                AdminActionTargetType.EQUIPMENT,
                EQUIPMENT_ID,
                actionType,
                reason,
                createdAt
        );
    }
}
