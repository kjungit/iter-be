package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

// 기술검토.md 1-1 POC: 같은 장비·겹치는 기간에 동시에 승인 요청이 들어와도
// 정확히 1건만 APPROVED, 나머지는 자동 REJECTED+REFUNDED되는지 실제 DB(H2) 위에서 검증한다.
// approveRental의 equipment SELECT ... FOR UPDATE 락이 의도대로 트랜잭션을 직렬화하는지가 핵심이라
// Mockito가 아닌 @SpringBootTest로 실제 트랜잭션 동시성을 확인한다.
@ActiveProfiles("test")
@SpringBootTest
class RentalApprovalConcurrencyTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RentalService rentalService;

    @Test
    void 겹치는_기간의_REQUESTED_예약이_동시에_승인요청되면_하나만_승인되고_나머지는_자동거절환불된다() throws InterruptedException {
        BigDecimal totalPrice = BigDecimal.valueOf(180000);
        int competitorCount = 3;

        User owner = userRepository.save(user("owner-" + System.nanoTime()));
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .ownerId(owner.getId())
                .category(EquipmentCategory.CAMERA)
                .name("소니 A7C2")
                .dailyPrice(BigDecimal.valueOf(30000))
                .build());

        List<User> renters = List.of(
                userRepository.save(user("renter1-" + System.nanoTime())),
                userRepository.save(user("renter2-" + System.nanoTime())),
                userRepository.save(user("renter3-" + System.nanoTime())));

        List<Rental> rentals = renters.stream()
                .map(renter -> rentalRepository.save(Rental.builder()
                        .equipmentId(equipment.getId())
                        .renterId(renter.getId())
                        .startDate(LocalDate.now().plusDays(10))
                        .endDate(LocalDate.now().plusDays(15))
                        .productNameSnapshot(equipment.getName())
                        .categorySnapshot(equipment.getCategory().name())
                        .dailyPriceSnapshot(equipment.getDailyPrice())
                        .rentalDays(6)
                        .totalPrice(totalPrice)
                        .status(RentalStatus.REQUESTED)
                        .build()))
                .toList();

        rentals.forEach(rental -> paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .amount(totalPrice)
                .status(PaymentStatus.PAID)
                .build()));

        ExecutorService executor = Executors.newFixedThreadPool(competitorCount);
        CountDownLatch ready = new CountDownLatch(competitorCount);
        CountDownLatch start = new CountDownLatch(1);

        List<CompletableFuture<Boolean>> futures = rentals.stream()
                .map(rental -> CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    awaitUninterruptibly(start);
                    try {
                        rentalService.approveRental(rental.getId(), owner.getId(), false);
                        return true;
                    } catch (CustomException e) {
                        return false;
                    }
                }, executor))
                .toList();

        ready.await();
        start.countDown();
        List<Boolean> results = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        List<Rental> finalRentals = rentalRepository.findAllById(rentals.stream().map(Rental::getId).toList());
        assertThat(finalRentals.stream().filter(r -> r.getStatus() == RentalStatus.APPROVED).count()).isEqualTo(1);
        assertThat(finalRentals.stream().filter(r -> r.getStatus() == RentalStatus.REJECTED).count())
                .isEqualTo(competitorCount - 1);

        for (Rental rental : finalRentals) {
            Payment payment = paymentRepository.findByRentalId(rental.getId()).orElseThrow();
            User renter = userRepository.findById(rental.getRenterId()).orElseThrow();
            if (rental.getStatus() == RentalStatus.APPROVED) {
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(renter.getPointBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            } else {
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
                // 이중환불이 없었는지: 정확히 totalPrice 1회분만 돌아와야 한다
                assertThat(renter.getPointBalance()).isEqualByComparingTo(totalPrice);
            }
        }
    }

    private User user(String tag) {
        return User.builder()
                .email(tag + "@test.com")
                .password("test-password")
                .name("동시성테스트")
                .pointBalance(BigDecimal.ZERO)
                .build();
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
