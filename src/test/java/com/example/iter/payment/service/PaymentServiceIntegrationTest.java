package com.example.iter.payment.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.client.TossPaymentClient;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.dto.request.PaymentConfirmRequest;
import com.example.iter.payment.dto.toss.TossConfirmApiResponse;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// PaymentService.ready()/confirm()에 @Transactional이 없으면, open-in-view: false 환경에서
// 레포지토리 조회로 얻은 엔티티가 detached 상태가 되어 markPaid()/changeStatus() 같은 변경이
// DB에 반영되지 않는 회귀를 방지한다 — 반드시 "변경 후 재조회"로 실제 DB 반영 여부를 확인해야
// Mockito 단위 테스트(PaymentServiceTest)로는 잡을 수 없는 이 문제를 잡을 수 있다.
@ActiveProfiles("test")
@SpringBootTest
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @Test
    void confirm_이후_재조회하면_실제_DB에_PAID_REQUESTED로_반영돼있다() {
        User owner = userRepository.save(user("owner"));
        User renter = userRepository.save(user("renter"));
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .ownerId(owner.getId()).category("카메라").name("A7C2")
                .dailyPrice(BigDecimal.valueOf(10000)).build());
        Rental rental = rentalRepository.save(Rental.builder()
                .equipmentId(equipment.getId()).renterId(renter.getId())
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(2))
                .productNameSnapshot("A7C2").categorySnapshot("카메라")
                .dailyPriceSnapshot(BigDecimal.valueOf(10000))
                .rentalDays(1).totalPrice(BigDecimal.valueOf(10000))
                .receiverName("n").receiverPhone("p").zipcode("z").address("a").detailAddress("d")
                .status(RentalStatus.PENDING)
                .build());

        paymentService.ready(rental.getId(), renter.getId());
        Payment readyPayment = paymentRepository.findByRentalId(rental.getId()).orElseThrow();

        when(tossPaymentClient.confirm(anyString(), anyString(), any(), anyString()))
                .thenReturn(new TossConfirmApiResponse(
                        "toss-payment-key", readyPayment.getOrderId(), "DONE",
                        OffsetDateTime.now().toString(), 10000L));

        paymentService.confirm(rental.getId(), renter.getId(),
                new PaymentConfirmRequest("toss-payment-key", readyPayment.getOrderId(), readyPayment.getAmount()));

        // 서비스가 리턴한 DTO가 아니라, 별도로 다시 조회해서 "진짜 DB 값"을 확인한다.
        Payment reloadedPayment = paymentRepository.findByRentalId(rental.getId()).orElseThrow();
        Rental reloadedRental = rentalRepository.findById(rental.getId()).orElseThrow();

        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(reloadedRental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
    }

    private User user(String tag) {
        return User.builder()
                .email(tag + "-" + System.nanoTime() + "@test.com")
                .password("test-password")
                .name(tag)
                .pointBalance(BigDecimal.ZERO)
                .build();
    }
}
