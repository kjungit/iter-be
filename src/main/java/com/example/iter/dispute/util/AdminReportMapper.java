package com.example.iter.dispute.util;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.dto.response.AdminReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminReportMapper {

    private final ReportMapper reportMapper;

    // 신고와 신고자 정보를 관리자 신고 목록 응답으로 변환합니다.
    public ReportSummaryResponse toSummary(Report report, User reporter) {
        return reportMapper.toSummary(report, reporter);
    }

    // 기본 신고 상세 정보에 관리자 처리 메모와 수정 시각을 추가합니다.
    public AdminReportDetailResponse toDetail(Report report, User reporter) {
        return new AdminReportDetailResponse(
                reportMapper.toDetail(report, reporter),
                report.getAdminMemo(),
                report.getUpdatedAt()
        );
    }
}
