package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
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

// 선점(first-come) 방식 검증: 같은 장비·겹치는 기간에 여러 회원이 동시에 대여 요청을 보내도
// 정확히 1건만 생성되고 나머지는 RENTAL_PERIOD_CONFLICT로 실패하는지 실제 DB(H2) 위에서 검증한다.
// createRental()의 equipment SELECT ... FOR UPDATE 락이 "겹침 확인 + 저장"을 원자적으로
// 직렬화하는지가 핵심이라 Mockito가 아닌 @SpringBootTest로 실제 트랜잭션 동시성을 확인한다.
@ActiveProfiles("test")
@SpringBootTest
class RentalCreationConcurrencyTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private RentalService rentalService;

    @Test
    void 같은_기간에_동시에_대여요청을_보내면_한_건만_성공한다() throws InterruptedException {
        int requesterCount = 3;

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

        RentalCreateRequest request = new RentalCreateRequest(
                equipment.getId(), LocalDate.now().plusDays(10), LocalDate.now().plusDays(15),
                "홍길동", "010-0000-0000", "12345", "서울시", "101동", "문 앞", true);

        ExecutorService executor = Executors.newFixedThreadPool(requesterCount);
        CountDownLatch ready = new CountDownLatch(requesterCount);
        CountDownLatch start = new CountDownLatch(1);

        List<CompletableFuture<Boolean>> futures = renters.stream()
                .map(renter -> CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    awaitUninterruptibly(start);
                    try {
                        rentalService.createRental(renter.getId(), request);
                        return true;
                    } catch (CustomException e) {
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RENTAL_PERIOD_CONFLICT);
                        return false;
                    }
                }, executor))
                .toList();

        ready.await();
        start.countDown();
        List<Boolean> results = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        List<Rental> createdRentals = rentalRepository.findAll().stream()
                .filter(r -> r.getEquipmentId().equals(equipment.getId()))
                .toList();
        assertThat(createdRentals).hasSize(1);
        assertThat(createdRentals.get(0).getStatus()).isEqualTo(RentalStatus.PENDING);
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
