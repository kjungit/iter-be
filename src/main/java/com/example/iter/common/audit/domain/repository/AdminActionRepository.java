package com.example.iter.common.audit.domain.repository;

import com.example.iter.common.audit.domain.entity.AdminAction;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {

    // 관리자 처리 이력을 대상 유형, 대상 ID, 조치 유형으로 조회합니다.전달되지 않은 조건은 조회에 적용하지 않습니다.
    @Query("""
                select a
                from AdminAction a
                where (
                        :targetType is null
                        or a.targetType = :targetType
                      )
                  and (
                        :targetId is null
                        or a.targetId = :targetId
                      )
                  and (
                        :action is null
                        or a.action = :action
                      )
                  and (
                        :cursorCreatedAt is null
                        or a.createdAt < :cursorCreatedAt
                        or (a.createdAt = :cursorCreatedAt and a.id < :cursorId)
                      )
                order by a.createdAt desc, a.id desc
                """)
    List<AdminAction> searchForAdminByCursor(
            @Param("targetType") AdminActionTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("action") AdminActionType action,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
