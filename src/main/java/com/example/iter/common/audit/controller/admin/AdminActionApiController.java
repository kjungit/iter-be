package com.example.iter.common.audit.controller.admin;

import com.example.iter.common.audit.controller.admin.spec.AdminActionApiSpec;
import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.audit.service.AdminActionQueryService;
import com.example.iter.common.dto.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/actions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActionApiController implements AdminActionApiSpec {

    private final AdminActionQueryService adminActionQueryService;

    // 관리자가 대상 유형, 대상 ID, 조치 유형 조건으로 처리 이력을 조회합니다.
    @Override
    @GetMapping
    public ResponseEntity<CursorPageResponse<AdminActionResponse>> getAdminActions(@ModelAttribute AdminActionSearchRequest request) {
        return ResponseEntity.ok(adminActionQueryService.getAdminActions(request));
    }
}
