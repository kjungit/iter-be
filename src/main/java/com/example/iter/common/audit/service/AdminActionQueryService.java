package com.example.iter.common.audit.service;

import com.example.iter.common.audit.domain.repository.AdminActionRepository;
import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.audit.util.AdminActionMapper;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminActionQueryService {

    private final AdminActionRepository adminActionRepository;
    private final AdminActionMapper adminActionMapper;

    // 관리자가 검색 조건과 커서 정보로 전체 처리 이력을 조회합니다.
    @Transactional(readOnly = true)
    public CursorPageResponse<AdminActionResponse> getAdminActions(AdminActionSearchRequest request) {
        CursorKey cursorKey = CursorCodec.decode(request.cursor());

        var actions = adminActionRepository.searchForAdminByCursor(
                request.targetType(),
                request.targetId(),
                request.action(),
                cursorKey == null ? null : cursorKey.createdAt(),
                cursorKey == null ? null : cursorKey.id(),
                PageRequest.of(0, request.size() + 1)
        );

        return CursorPageResponse.from(
                actions,
                request.size(),
                adminActionMapper::toResponse,
                action -> new CursorKey(action.getCreatedAt(), action.getId())
        );
    }
}
