package com.example.iter.payment.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.exception.PointInsufficientException;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Rental pendingRental() {
        return Rental.builder()
                .id(10L)
                .equipmentId(1L)
                .renterId(2L)
                .startDate(LocalDate.of(2026, 8, 20))
                .endDate(LocalDate.of(2026, 8, 25))
                .productNameSnapshot("소니 A7C2")
                .categorySnapshot("카메라")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(6)
                .totalPrice(BigDecimal.valueOf(180000))
                .status(RentalStatus.PENDING)
                .build();
    }

    @Test
    void 포인트가_충분하면_결제에_성공하고_상태가_바뀐다() {
        Rental rental = pendingRental();
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalId(10L)).thenReturn(Optional.empty());
        when(userRepository.deductPointBalance(2L, BigDecimal.valueOf(180000))).thenReturn(1);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                com.example.iter.auth.domain.entity.User.builder().id(2L).pointBalance(BigDecimal.valueOf(820000)).build()));

        var response = paymentService.payRental(10L, 2L);

        assertThat(response.rentalStatus()).isEqualTo(RentalStatus.REQUESTED);
        assertThat(response.pointBalance()).isEqualByComparingTo(BigDecimal.valueOf(820000));
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
    }

    @Test
    void 포인트가_부족하면_잔액과_필요금액을_담아_예외를_던진다() {
        Rental rental = pendingRental();
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalId(10L)).thenReturn(Optional.empty());
        when(userRepository.deductPointBalance(2L, BigDecimal.valueOf(180000))).thenReturn(0);
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                com.example.iter.auth.domain.entity.User.builder().id(2L).pointBalance(BigDecimal.valueOf(50000)).build()));

        assertThatThrownBy(() -> paymentService.payRental(10L, 2L))
                .isInstanceOf(PointInsufficientException.class)
                .satisfies(e -> {
                    PointInsufficientException ex = (PointInsufficientException) e;
                    assertThat(ex.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000));
                    assertThat(ex.getRequiredAmount()).isEqualByComparingTo(BigDecimal.valueOf(180000));
                });
    }

    @Test
    void 결제_대기_상태가_아니면_결제할_수_없다() {
        Rental rental = pendingRental();
        rental.changeStatus(RentalStatus.REQUESTED);
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental));

        assertThatThrownBy(() -> paymentService.payRental(10L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_NOT_PAYABLE);
    }
}
