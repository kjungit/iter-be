package com.example.iter.device.domain.repository;

import com.example.iter.common.audit.domain.entity.AdminAction;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.domain.repository.AdminActionRepository;
import com.example.iter.common.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AdminActionRepositoryAdminTest {

    @Autowired
    private AdminActionRepository adminActionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 대상_종류로_관리자_처리_이력을_검색한다() {
        AdminAction firstEquipmentAction = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "장비 차단"
        );
        saveAction(
                1L,
                AdminActionTargetType.USER,
                10L,
                AdminActionType.SUSPEND_USER,
                "회원 이용 정지"
        );
        AdminAction secondEquipmentAction = saveAction(
                2L,
                AdminActionTargetType.EQUIPMENT,
                20L,
                AdminActionType.RESTORE_EQUIPMENT,
                "장비 차단 해제"
        );

        var result = adminActionRepository.searchForAdmin(
                AdminActionTargetType.EQUIPMENT,
                null,
                null,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(
                        firstEquipmentAction.getId(),
                        secondEquipmentAction.getId()
                );
    }

    @Test
    void 대상_ID로_관리자_처리_이력을_검색한다() {
        AdminAction equipmentAction = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "장비 차단"
        );
        AdminAction userAction = saveAction(
                2L,
                AdminActionTargetType.USER,
                10L,
                AdminActionType.SUSPEND_USER,
                "회원 이용 정지"
        );
        saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                20L,
                AdminActionType.RESTORE_EQUIPMENT,
                "다른 장비 차단 해제"
        );

        var result = adminActionRepository.searchForAdmin(
                null,
                10L,
                null,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(
                        equipmentAction.getId(),
                        userAction.getId()
                );
    }

    @Test
    void 작업_유형으로_관리자_처리_이력을_검색한다() {
        saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "장비 차단"
        );
        AdminAction expected = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.RESTORE_EQUIPMENT,
                "장비 차단 해제"
        );
        saveAction(
                2L,
                AdminActionTargetType.USER,
                20L,
                AdminActionType.RESTORE_USER,
                "회원 이용 정지 해제"
        );

        var result = adminActionRepository.searchForAdmin(
                null,
                null,
                AdminActionType.RESTORE_EQUIPMENT,
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void 대상_종류와_대상_ID와_작업_유형을_모두_적용해_검색한다() {
        AdminAction expected = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "신고 누적으로 장비 차단"
        );
        saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.RESTORE_EQUIPMENT,
                "장비 차단 사유 해소"
        );
        saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                20L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "다른 장비 차단"
        );
        saveAction(
                1L,
                AdminActionTargetType.USER,
                10L,
                AdminActionType.SUSPEND_USER,
                "회원 이용 정지"
        );

        var result = adminActionRepository.searchForAdmin(
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(expected.getId());

        AdminAction found = result.getContent().getFirst();
        assertThat(found.getAdminId()).isEqualTo(1L);
        assertThat(found.getTargetType())
                .isEqualTo(AdminActionTargetType.EQUIPMENT);
        assertThat(found.getTargetId()).isEqualTo(10L);
        assertThat(found.getAction())
                .isEqualTo(AdminActionType.SUSPEND_EQUIPMENT);
        assertThat(found.getReason()).isEqualTo("신고 누적으로 장비 차단");
    }

    @Test
    void 검색_조건과_일치하는_처리_이력이_없으면_빈_페이지를_반환한다() {
        saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "장비 차단"
        );

        var result = adminActionRepository.searchForAdmin(
                AdminActionTargetType.USER,
                999L,
                AdminActionType.RESTORE_USER,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    void 검색_조건이_없으면_전체_처리_이력을_페이징한다() {
        AdminAction first = saveAction(
                1L,
                AdminActionTargetType.USER,
                10L,
                AdminActionType.SUSPEND_USER,
                "첫 번째 처리"
        );
        AdminAction second = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                20L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "두 번째 처리"
        );
        AdminAction third = saveAction(
                2L,
                AdminActionTargetType.REPORT,
                30L,
                AdminActionType.RESOLVE_REPORT,
                "세 번째 처리"
        );

        Sort idAscending = Sort.by(Sort.Direction.ASC, "id");

        var firstPage = adminActionRepository.searchForAdmin(
                null,
                null,
                null,
                PageRequest.of(0, 2, idAscending)
        );
        var secondPage = adminActionRepository.searchForAdmin(
                null,
                null,
                null,
                PageRequest.of(1, 2, idAscending)
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(first.getId(), second.getId());

        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(third.getId());
    }

    @Test
    void 마지막_페이지_다음은_전체_개수를_유지한_빈_페이지를_반환한다() {
        saveAction(
                1L,
                AdminActionTargetType.USER,
                10L,
                AdminActionType.SUSPEND_USER,
                "첫 번째 처리"
        );
        saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                20L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "두 번째 처리"
        );
        saveAction(
                2L,
                AdminActionTargetType.REPORT,
                30L,
                AdminActionType.RESOLVE_REPORT,
                "세 번째 처리"
        );

        var result = adminActionRepository.searchForAdmin(
                null,
                null,
                null,
                PageRequest.of(
                        2,
                        2,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    void 처리_시각_내림차순과_동일_시각의_ID_내림차순으로_정렬한다() {
        AdminAction olderAction = saveAction(
                1L,
                AdminActionTargetType.USER,
                10L,
                AdminActionType.SUSPEND_USER,
                "이전 처리"
        );
        AdminAction sameTimeFirstAction = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                20L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "동일 시각의 첫 번째 처리"
        );
        AdminAction sameTimeSecondAction = saveAction(
                1L,
                AdminActionTargetType.REPORT,
                30L,
                AdminActionType.RESOLVE_REPORT,
                "동일 시각의 두 번째 처리"
        );

        LocalDateTime olderCreatedAt =
                LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime latestCreatedAt =
                LocalDateTime.of(2026, 8, 2, 10, 0);

        updateCreatedAt(olderAction.getId(), olderCreatedAt);
        updateCreatedAt(sameTimeFirstAction.getId(), latestCreatedAt);
        updateCreatedAt(sameTimeSecondAction.getId(), latestCreatedAt);
        entityManager.clear();

        var result = adminActionRepository.searchForAdmin(
                null,
                null,
                null,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );

        assertThat(result.getContent())
                .extracting(AdminAction::getId)
                .containsExactly(
                        sameTimeSecondAction.getId(),
                        sameTimeFirstAction.getId(),
                        olderAction.getId()
                );

        assertThat(result.getContent())
                .extracting(AdminAction::getCreatedAt)
                .containsExactly(
                        latestCreatedAt,
                        latestCreatedAt,
                        olderCreatedAt
                );
    }

    @Test
    void 처리_이력을_저장하면_처리_시각이_자동으로_기록된다() {
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);

        AdminAction savedAction = saveAction(
                1L,
                AdminActionTargetType.EQUIPMENT,
                10L,
                AdminActionType.SUSPEND_EQUIPMENT,
                "처리 시각 기록 확인"
        );

        LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);
        Long savedActionId = savedAction.getId();
        entityManager.clear();

        AdminAction found = adminActionRepository.findById(savedActionId)
                .orElseThrow();

        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCreatedAt())
                .isBetween(beforeSave, afterSave);
        assertThat(found.getAdminId()).isEqualTo(1L);
        assertThat(found.getTargetType())
                .isEqualTo(AdminActionTargetType.EQUIPMENT);
        assertThat(found.getTargetId()).isEqualTo(10L);
        assertThat(found.getAction())
                .isEqualTo(AdminActionType.SUSPEND_EQUIPMENT);
        assertThat(found.getReason()).isEqualTo("처리 시각 기록 확인");
    }

    private AdminAction saveAction(
            Long adminId,
            AdminActionTargetType targetType,
            Long targetId,
            AdminActionType action,
            String reason
    ) {
        return adminActionRepository.saveAndFlush(
                AdminAction.builder()
                        .adminId(adminId)
                        .targetType(targetType)
                        .targetId(targetId)
                        .action(action)
                        .reason(reason)
                        .build()
        );
    }

    private void updateCreatedAt(
            Long actionId,
            LocalDateTime createdAt
    ) {
        entityManager.createQuery("""
                        update AdminAction a
                        set a.createdAt = :createdAt
                        where a.id = :actionId
                        """)
                .setParameter("createdAt", createdAt)
                .setParameter("actionId", actionId)
                .executeUpdate();
    }
}