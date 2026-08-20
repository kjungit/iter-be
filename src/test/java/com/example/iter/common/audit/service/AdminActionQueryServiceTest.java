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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    void 관리자_처리_이력을_검색하고_최신순으로_페이징해_조회한다() {
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
                1,
                10
        );
        PageRequest repositoryPageable = PageRequest.of(
                1,
                10,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        when(adminActionRepository.searchForAdmin(
                eq(AdminActionTargetType.EQUIPMENT),
                eq(EQUIPMENT_ID),
                eq(AdminActionType.SUSPEND_EQUIPMENT),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(firstAction, secondAction),
                repositoryPageable,
                12
        ));
        when(adminActionMapper.toResponse(firstAction))
                .thenReturn(firstResponse);
        when(adminActionMapper.toResponse(secondAction))
                .thenReturn(secondResponse);

        var result = adminActionQueryService.getAdminActions(request);

        assertThat(result.content()).containsExactly(
                firstResponse,
                secondResponse
        );
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(12);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionRepository).searchForAdmin(
                eq(AdminActionTargetType.EQUIPMENT),
                eq(EQUIPMENT_ID),
                eq(AdminActionType.SUSPEND_EQUIPMENT),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(1);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(capturedPageable.getSort().getOrderFor("id"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);

        verify(adminActionMapper).toResponse(firstAction);
        verify(adminActionMapper).toResponse(secondAction);
    }

    @Test
    void 검색_조건이_없으면_null을_전달하고_기본_페이지로_조회한다() {
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

        when(adminActionRepository.searchForAdmin(
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(action),
                PageRequest.of(0, 20),
                1
        ));
        when(adminActionMapper.toResponse(action))
                .thenReturn(mappedResponse);

        var result = adminActionQueryService.getAdminActions(request);

        assertThat(result.content()).containsExactly(mappedResponse);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionRepository).searchForAdmin(
                isNull(),
                isNull(),
                isNull(),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isZero();
        assertThat(capturedPageable.getPageSize()).isEqualTo(20);
        assertThat(capturedPageable.getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(capturedPageable.getSort().getOrderFor("id"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);

        verify(adminActionMapper).toResponse(action);
    }

    @Test
    void 처리_이력_검색_결과가_비어있으면_매퍼를_호출하지_않는다() {
        AdminActionSearchRequest request = new AdminActionSearchRequest(
                AdminActionTargetType.USER,
                20L,
                AdminActionType.SUSPEND_USER,
                2,
                5
        );

        when(adminActionRepository.searchForAdmin(
                eq(AdminActionTargetType.USER),
                eq(20L),
                eq(AdminActionType.SUSPEND_USER),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(2, 5),
                0
        ));

        var result = adminActionQueryService.getAdminActions(request);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isZero();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionRepository).searchForAdmin(
                eq(AdminActionTargetType.USER),
                eq(20L),
                eq(AdminActionType.SUSPEND_USER),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
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