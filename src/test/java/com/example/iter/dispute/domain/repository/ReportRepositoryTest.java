package com.example.iter.dispute.domain.repository;

import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ReportRepositoryTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long OTHER_REPORTER_ID = 2L;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void 신고_ID와_신고자_ID가_모두_일치해야_상세를_조회할_수_있다() {
        Report savedReport = reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.USER,
                10L,
                ReportStatus.RECEIVED
        ));

        assertThat(reportRepository.findByIdAndReporterId(savedReport.getId(), REPORTER_ID))
                .isPresent();
        assertThat(reportRepository.findByIdAndReporterId(savedReport.getId(), OTHER_REPORTER_ID))
                .isEmpty();
    }

    @Test
    void 내_신고만_대상_유형과_상태로_필터링해_조회한다() {
        Report matchingOld = reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                ReportStatus.RECEIVED
        ));
        Report matchingNew = reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                101L,
                ReportStatus.RECEIVED
        ));
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.USER,
                10L,
                ReportStatus.RECEIVED
        ));
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                102L,
                ReportStatus.RESOLVED
        ));
        reportRepository.saveAndFlush(report(
                OTHER_REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                103L,
                ReportStatus.RECEIVED
        ));

        var pageable = PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        var result = reportRepository.searchMyReports(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                ReportStatus.RECEIVED,
                pageable
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Report::getId)
                .containsExactly(matchingNew.getId(), matchingOld.getId());
    }

    @Test
    void 검색_조건이_없으면_본인의_전체_신고만_조회한다() {
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.USER,
                10L,
                ReportStatus.RECEIVED
        ));
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.RENTAL,
                20L,
                ReportStatus.REJECTED
        ));
        reportRepository.saveAndFlush(report(
                OTHER_REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                30L,
                ReportStatus.UNDER_REVIEW
        ));

        var result = reportRepository.searchMyReports(
                REPORTER_ID,
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allMatch(report -> report.getReporterId().equals(REPORTER_ID));
    }

    @Test
    void 접수_또는_검토중인_동일_대상_신고가_있으면_처리중_신고로_판단한다() {
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                ReportStatus.RECEIVED
        ));
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                101L,
                ReportStatus.UNDER_REVIEW
        ));

        var activeStatuses = EnumSet.of(
                ReportStatus.RECEIVED,
                ReportStatus.UNDER_REVIEW
        );

        assertThat(reportRepository.existsActiveReport(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                activeStatuses
        )).isTrue();
        assertThat(reportRepository.existsActiveReport(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                101L,
                activeStatuses
        )).isTrue();
        assertThat(reportRepository.existsActiveReport(
                OTHER_REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                activeStatuses
        )).isFalse();
        assertThat(reportRepository.existsActiveReport(
                REPORTER_ID,
                ReportTargetType.USER,
                100L,
                activeStatuses
        )).isFalse();
        assertThat(reportRepository.existsActiveReport(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                999L,
                activeStatuses
        )).isFalse();
    }

    @Test
    void 해결되거나_기각된_신고만_있으면_같은_대상을_다시_신고할_수_있다() {
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                ReportStatus.RESOLVED
        ));
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                ReportStatus.REJECTED
        ));

        boolean activeReportExists = reportRepository.existsActiveReport(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                100L,
                EnumSet.of(ReportStatus.RECEIVED, ReportStatus.UNDER_REVIEW)
        );

        assertThat(activeReportExists).isFalse();
    }

    @Test
    void 대상_유형과_대상_ID가_같은_신고_수를_신고자와_관계없이_집계한다() {
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.USER,
                10L,
                ReportStatus.RECEIVED
        ));
        reportRepository.saveAndFlush(report(
                OTHER_REPORTER_ID,
                ReportTargetType.USER,
                10L,
                ReportStatus.RESOLVED
        ));
        reportRepository.saveAndFlush(report(
                REPORTER_ID,
                ReportTargetType.EQUIPMENT,
                10L,
                ReportStatus.RECEIVED
        ));

        assertThat(reportRepository.countByTargetTypeAndTargetId(
                ReportTargetType.USER,
                10L
        )).isEqualTo(2);
    }

    private Report report(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    ) {
        return Report.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reason("테스트 신고 사유")
                .description("테스트 신고 내용입니다.")
                .status(status)
                .build();
    }
}
