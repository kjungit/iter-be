package com.example.iter.common.audit.service;

import com.example.iter.common.audit.domain.entity.AdminAction;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.domain.repository.AdminActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 도메인(회원/장비/분쟁 관리 등)에서 조치를 취할 때마다 이 서비스를 호출해 감사 로그를 남긴다.
@Service
@RequiredArgsConstructor
public class AdminActionService {

    private final AdminActionRepository adminActionRepository;

    @Transactional
    public void record(Long adminId, AdminActionTargetType targetType, Long targetId, AdminActionType action, String reason) {
        AdminAction adminAction = AdminAction.builder()
                .adminId(adminId)
                .targetType(targetType)
                .targetId(targetId)
                .action(action)
                .reason(reason)
                .build();
        adminActionRepository.save(adminAction);
    }
}
