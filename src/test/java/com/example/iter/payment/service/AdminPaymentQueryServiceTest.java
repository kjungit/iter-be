package com.example.iter.payment.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.AdminPaymentQueryRepository;
import com.example.iter.payment.dto.request.AdminPaymentSearchRequest;
import com.example.iter.payment.service.model.AdminPaymentDetailRow;
import com.example.iter.payment.service.model.AdminPaymentSummaryRow;
import com.example.iter.payment.util.AdminPaymentMapper;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentQueryServiceTest {

    private static final Long PAYMENT_ID = 10L;

    @Mock
    private AdminPaymentQueryRepository adminPaymentQueryRepository;

    @Spy
    private AdminPaymentMapper adminPaymentMapper = new AdminPaymentMapper();

    @InjectMocks
    private AdminPaymentQueryService adminPaymentQueryService;

    @Test
    void 결제_목록은_검색어와_날짜와_커서를_정규화해_조회한다() {
        CursorKey cursorKey = new CursorKey(LocalDateTime.of(2026, 8, 25, 10, 0), 50L);
        AdminPaymentSearchRequest request = new AdminPaymentSearchRequest(
                "  맥북  ",
                PaymentStatus.PAID,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                CursorCodec.encode(cursorKey),
                10
        );
        AdminPaymentSummaryRow row = summaryRow();
        when(adminPaymentQueryRepository.searchForAdminByCursor(
                eq("맥북"),
                eq(PaymentStatus.PAID),
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(cursorKey.createdAt()),
                eq(cursorKey.id()),
                any(Pageable.class)
        )).thenReturn(List.of(row));

        var response = adminPaymentQueryService.getPayments(request);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.content().getFirst().equipmentName()).isEqualTo("맥북 프로");
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminPaymentQueryRepository).searchForAdminByCursor(
                eq("맥북"),
                eq(PaymentStatus.PAID),
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(cursorKey.createdAt()),
                eq(cursorKey.id()),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(11);
    }

    @Test
    void 빈_검색어와_날짜와_커서는_null로_전달하고_기본_크기를_사용한다() {
        when(adminPaymentQueryRepository.searchWithoutKeywordForAdminByCursor(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of());

        var response = adminPaymentQueryService.getPayments(
                new AdminPaymentSearchRequest("  ", null, null, null, null, null)
        );

        assertThat(response.content()).isEmpty();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.hasNext()).isFalse();
        verify(adminPaymentQueryRepository).searchWithoutKeywordForAdminByCursor(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void 퍼센트와_언더스코어_검색어를_변경하지_않고_전달한다() {
        when(adminPaymentQueryRepository.searchForAdminByCursor(
                eq("%_"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of());

        adminPaymentQueryService.getPayments(
                new AdminPaymentSearchRequest("  %_  ", null, null, null, null, 20)
        );

        verify(adminPaymentQueryRepository).searchForAdminByCursor(
                eq("%_"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void 결제_상세를_조회해_응답으로_변환한다() {
        when(adminPaymentQueryRepository.findDetailById(PAYMENT_ID))
                .thenReturn(Optional.of(detailRow()));

        var response = adminPaymentQueryService.getPayment(PAYMENT_ID);

        assertThat(response.payment().paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.payment().renterEmail()).isEqualTo("renter@iter.test");
        assertThat(response.rental().equipmentId()).isEqualTo(30L);
        assertThat(response.rental().equipmentName()).isEqualTo("맥북 프로");
        assertThat(response.rental().dailyPrice()).isEqualByComparingTo("30000");
    }

    @Test
    void 존재하지_않는_결제_상세는_PAYMENT_NOT_FOUND를_발생시킨다() {
        when(adminPaymentQueryRepository.findDetailById(PAYMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPaymentQueryService.getPayment(PAYMENT_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);

        verifyNoInteractions(adminPaymentMapper);
    }

    private AdminPaymentSummaryRow summaryRow() {
        return new AdminPaymentSummaryRow(
                PAYMENT_ID,
                20L,
                "ORDER-001",
                2L,
                "renter@iter.test",
                "대여자",
                "렌터",
                "맥북 프로",
                BigDecimal.valueOf(300000),
                PaymentStatus.PAID,
                RentalStatus.REQUESTED,
                LocalDateTime.of(2026, 8, 20, 10, 30),
                null,
                LocalDateTime.of(2026, 8, 20, 10, 0)
        );
    }

    private AdminPaymentDetailRow detailRow() {
        return new AdminPaymentDetailRow(
                PAYMENT_ID,
                20L,
                "ORDER-001",
                2L,
                "renter@iter.test",
                "대여자",
                "렌터",
                30L,
                "맥북 프로",
                "LAPTOP",
                BigDecimal.valueOf(30000),
                LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 8, 30),
                10,
                BigDecimal.valueOf(300000),
                BigDecimal.valueOf(300000),
                PaymentStatus.PAID,
                RentalStatus.REQUESTED,
                LocalDateTime.of(2026, 8, 20, 10, 30),
                null,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 20, 10, 30)
        );
    }
}
