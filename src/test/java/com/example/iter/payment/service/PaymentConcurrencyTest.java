package com.example.iter.payment.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.payment.exception.PointInsufficientException;
import com.example.iter.reservation.domain.entity.Rental;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// 기술검토.md 1-2 권장: 같은 사용자가 여러 예약을 동시에 결제해도 point_balance가 음수가 되지 않는지 검증.
// UserRepository.deductPointBalance의 조건부 UPDATE(WHERE point_balance >= amount)가 실제 DB 위에서
// 동시 요청을 원자적으로 막아주는지 확인하는 게 목적이라 Mockito가 아닌 실제 DB(H2, test 프로파일)를 사용한다.
@ActiveProfiles("test")
@SpringBootTest
class PaymentConcurrencyTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private PaymentService paymentService;

    @Test
    void 동시_결제_요청이_와도_포인트_잔액은_음수가_되지_않는다() throws InterruptedException {
        BigDecimal initialBalance = BigDecimal.valueOf(100000);
        BigDecimal pricePerRental = BigDecimal.valueOf(60000);
        int rentalCount = 3; // 100,000 / 60,000 -> 정확히 1건만 성공해야 함

        User renter = userRepository.save(User.builder()
                .email("concurrency-" + System.nanoTime() + "@test.com")
                .password("test-password")
                .name("동시성테스트")
                .pointBalance(initialBalance)
                .build());

        List<Rental> rentals = IntStream.range(0, rentalCount)
                .mapToObj(i -> rentalRepository.save(Rental.builder()
                        .equipmentId(1L)
                        .renterId(renter.getId())
                        .startDate(LocalDate.now().plusDays(i))
                        .endDate(LocalDate.now().plusDays(i + 1))
                        .productNameSnapshot("동시성 테스트 장비")
                        .categorySnapshot("기타")
                        .dailyPriceSnapshot(pricePerRental)
                        .rentalDays(1)
                        .totalPrice(pricePerRental)
                        .build()))
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(rentalCount);
        CountDownLatch ready = new CountDownLatch(rentalCount);
        CountDownLatch start = new CountDownLatch(1);

        List<CompletableFuture<Boolean>> futures = rentals.stream()
                .map(rental -> CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    awaitUninterruptibly(start);
                    try {
                        paymentService.payRental(rental.getId(), renter.getId());
                        return true;
                    } catch (PointInsufficientException e) {
                        return false;
                    }
                }, executor))
                .toList();

        ready.await();
        start.countDown();
        List<Boolean> results = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        long successCount = results.stream().filter(Boolean::booleanValue).count();
        assertThat(successCount).isEqualTo(1);

        BigDecimal finalBalance = userRepository.findById(renter.getId()).orElseThrow().getPointBalance();
        assertThat(finalBalance).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(finalBalance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
