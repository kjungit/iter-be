package com.example.iter.common.audit.dto.response;

import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;

import java.time.LocalDateTime;

public record AdminActionResponse(
        Long actionId,
        Long adminId,
        AdminActionTargetType targetType,
        Long targetId,
        AdminActionType action,
        String reason,
        LocalDateTime createdAt
) {
}
