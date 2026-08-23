package com.example.iter.common.stats.controller.admin;

import com.example.iter.common.stats.controller.admin.spec.AdminStatsApiSpec;
import com.example.iter.common.stats.dto.response.AdminStatsResponse;
import com.example.iter.common.stats.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsApiController implements AdminStatsApiSpec {

    private final AdminStatsService adminStatsService;

    // 관리자 대시보드 상단에 표시할 전체 회원·장비·접수된 신고·결제 건수를 조회합니다.
    @Override
    @GetMapping
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }
}
