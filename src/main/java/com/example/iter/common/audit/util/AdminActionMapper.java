package com.example.iter.common.audit.util;

import com.example.iter.common.audit.domain.entity.AdminAction;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminActionMapper {

    // 관리자 처리 이력 엔티티를 처리 이력 응답으로 변환합니다.
    public AdminActionResponse toResponse(AdminAction adminAction) {
        return new AdminActionResponse(
                adminAction.getId(),
                adminAction.getAdminId(),
                adminAction.getTargetType(),
                adminAction.getTargetId(),
                adminAction.getAction(),
                adminAction.getReason(),
                adminAction.getCreatedAt()
        );
    }
}
