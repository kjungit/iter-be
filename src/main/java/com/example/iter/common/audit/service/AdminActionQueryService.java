package com.example.iter.common.audit.service;

import com.example.iter.common.audit.domain.repository.AdminActionRepository;
import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.audit.util.AdminActionMapper;
import com.example.iter.common.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminActionQueryService {

    private final AdminActionRepository adminActionRepository;
    private final AdminActionMapper adminActionMapper;

    // 관리자가 검색 조건과 페이지 정보로 전체 처리 이력을 조회합니다.
    @Transactional(readOnly = true)
    public PageResponse<AdminActionResponse> getAdminActions(AdminActionSearchRequest request) {
        PageRequest pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<AdminActionResponse> responsePage = adminActionRepository.searchForAdmin(
                request.targetType(),
                request.targetId(),
                request.action(),
                pageable
        ).map(adminActionMapper::toResponse);

        return PageResponse.from(responsePage);
    }
}
