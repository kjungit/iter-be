package com.example.iter.common.audit.controller.admin;

import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.audit.service.AdminActionQueryService;
import com.example.iter.common.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Action", description = "관리자 처리 이력 조회 API")
@Validated
@RestController
@RequestMapping("/api/v1/admin/actions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "JWT")
public class AdminActionApiController {

    private final AdminActionQueryService adminActionQueryService;

    // 관리자가 대상 유형, 대상 ID, 조치 유형 조건으로 처리 이력을 조회합니다.
    @Operation(summary = "관리자 처리 이력 조회")
    @GetMapping
    public ResponseEntity<PageResponse<AdminActionResponse>> getAdminActions(
            @Valid @ModelAttribute AdminActionSearchRequest request
    ) {
        return ResponseEntity.ok(adminActionQueryService.getAdminActions(request));
    }
}
