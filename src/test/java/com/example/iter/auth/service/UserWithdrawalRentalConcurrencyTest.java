package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.UserDeleteRequest;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
import com.example.iter.reservation.service.RentalService;
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

@ActiveProfiles("test")
@SpringBootTest
class UserWithdrawalRentalConcurrencyTest {

    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private RentalService rentalService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;

    @Test
    void 대여자_탈퇴와_신규_대여가_동시에_실행돼도_탈퇴_회원에게_대여가_남지_않는다() throws Exception {
        User owner = saveUser("renter-race-owner");
        User renter = saveUser("renter-race-renter");
        Equipment equipment = saveEquipment(owner.getId());

        RaceResult result = race(
                () -> userAccountService.withdraw(renter.getId(), new UserDeleteRequest(null)),
                () -> rentalService.createRental(renter.getId(), request(equipment.getId()))
        );

        assertRaceInvariant(result, renter.getId(), equipment.getId(), ErrorCode.USER_DELETED);
    }

    @Test
    void 장비_소유자_탈퇴와_신규_대여가_동시에_실행돼도_탈퇴_회원의_장비에_대여가_남지_않는다()
            throws Exception {
        User owner = saveUser("owner-race-owner");
        User renter = saveUser("owner-race-renter");
        Equipment equipment = saveEquipment(owner.getId());

        RaceResult result = race(
                () -> userAccountService.withdraw(owner.getId(), new UserDeleteRequest(null)),
                () -> rentalService.createRental(renter.getId(), request(equipment.getId()))
        );

        assertRaceInvariant(result, owner.getId(), equipment.getId(), ErrorCode.EQUIPMENT_NOT_AVAILABLE);
    }

    private RaceResult race(Runnable withdrawal, Runnable rentalCreation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            CompletableFuture<Attempt> withdrawalFuture = CompletableFuture.supplyAsync(
                    () -> attempt(ready, start, withdrawal), executor);
            CompletableFuture<Attempt> rentalFuture = CompletableFuture.supplyAsync(
                    () -> attempt(ready, start, rentalCreation), executor);

            ready.await();
            start.countDown();
            return new RaceResult(withdrawalFuture.join(), rentalFuture.join());
        } finally {
            executor.shutdown();
        }
    }

    private Attempt attempt(CountDownLatch ready, CountDownLatch start, Runnable operation) {
        ready.countDown();
        awaitUninterruptibly(start);
        try {
            operation.run();
            return new Attempt(true, null);
        } catch (CustomException e) {
            return new Attempt(false, e.getErrorCode());
        }
    }

    private void assertRaceInvariant(
            RaceResult result,
            Long withdrawingUserId,
            Long equipmentId,
            ErrorCode rentalFailureCode
    ) {
        assertThat(List.of(result.withdrawal(), result.rentalCreation()))
                .filteredOn(Attempt::success)
                .hasSize(1);

        User user = userRepository.findById(withdrawingUserId).orElseThrow();
        List<Rental> rentals = rentalRepository.findAll().stream()
                .filter(rental -> rental.getEquipmentId().equals(equipmentId))
                .toList();

        if (result.withdrawal().success()) {
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(rentals).isEmpty();
            assertThat(result.rentalCreation().errorCode()).isEqualTo(rentalFailureCode);
        } else {
            assertThat(result.withdrawal().errorCode()).isEqualTo(ErrorCode.ACTIVE_RENTAL_EXISTS);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(rentals).hasSize(1);
        }
    }

    private User saveUser(String tag) {
        return userRepository.saveAndFlush(User.builder()
                .email(tag + "-" + System.nanoTime() + "@example.com")
                .password(null)
                .name("동시성 테스트 회원")
                .nickname(tag)
                .phone("010-1111-2222")
                .build());
    }

    private Equipment saveEquipment(Long ownerId) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(ownerId)
                .category(EquipmentCategory.CAMERA)
                .name("동시성 테스트 장비")
                .description("회원 탈퇴와 대여 생성 동시성 테스트")
                .dailyPrice(BigDecimal.valueOf(10_000))
                .availableFrom(LocalDate.now().plusDays(1))
                .availableTo(LocalDate.now().plusMonths(1))
                .status(EquipmentStatus.ACTIVE)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private RentalCreateRequest request(Long equipmentId) {
        return new RentalCreateRequest(
                equipmentId,
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(5),
                "수령인",
                "010-2222-3333",
                "12345",
                "서울시 테스트구",
                "101호",
                "문 앞에 놓아주세요",
                true
        );
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private record Attempt(boolean success, ErrorCode errorCode) {
    }

    private record RaceResult(Attempt withdrawal, Attempt rentalCreation) {
    }
}
