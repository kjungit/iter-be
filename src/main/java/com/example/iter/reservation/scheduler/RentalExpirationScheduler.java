package com.example.iter.reservation.scheduler;

import com.example.iter.reservation.service.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 결제(confirm)를 30분 안에 완료하지 않은 PENDING 요청을 주기적으로 자동 취소해서 선점을 풀어준다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalExpirationScheduler {

    private final RentalService rentalService;

    @Scheduled(fixedRate = 60_000)
    public void expirePendingRentals() {
        int expiredCount = rentalService.expirePendingRentals();
        if (expiredCount > 0) {
            log.info("30분 초과 미결제 대여요청 {}건 자동 취소", expiredCount);
        }
    }
}
