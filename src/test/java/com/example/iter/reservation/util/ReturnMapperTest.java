package com.example.iter.reservation.util;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.reservation.domain.entity.ProductConditionType;
import com.example.iter.reservation.domain.entity.Receipt;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.entity.ReturnReceipt;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnMapperTest {

    private final ReturnMapper returnMapper = new ReturnMapper();

    @Test
    void 거래와_대여자_반납정보를_반납_확인_대상으로_변환한다() {
        Rental rental = rental(RentalStatus.RETURNED);
        User renter = renter();
        ReturnReceipt returnReceipt = returnReceipt(rental);

        var response = returnMapper.toTarget(
                rental,
                renter,
                "https://example.com/thumbnail.jpg",
                returnReceipt
        );

        assertThat(response.rentalId()).isEqualTo(10L);
        assertThat(response.equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(response.thumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
        assertThat(response.renter().userId()).isEqualTo(2L);
        assertThat(response.renter().nickName()).isEqualTo("대여자닉네임");
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(response.returnDate()).isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    void 수령과_반납_증빙을_비교_응답으로_변환한다() {
        Rental rental = rental(RentalStatus.RETURNED);
        Receipt receipt = receipt(rental);
        ReturnReceipt returnReceipt = returnReceipt(rental);
        LocalDateTime returnedAt = LocalDateTime.of(2026, 8, 11, 17, 20);
        ReflectionTestUtils.setField(returnReceipt, "createdAt", returnedAt);

        var response = returnMapper.toComparison(
                rental,
                renter(),
                receipt,
                List.of("receipt-1.jpg", "receipt-2.jpg"),
                returnReceipt,
                List.of("return-1.jpg")
        );

        assertThat(response.rentalId()).isEqualTo(10L);
        assertThat(response.equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(response.returnDate()).isEqualTo(LocalDate.of(2026, 8, 11));
        assertThat(response.receipt().productCondition()).isEqualTo(ProductConditionType.NORMAL);
        assertThat(response.receipt().conditionDetail()).isEqualTo("수령 시 정상");
        assertThat(response.receipt().imageUrls()).containsExactly("receipt-1.jpg", "receipt-2.jpg");
        assertThat(response.receipt().recordedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 14, 30));
        assertThat(response.returnReceipt().productCondition()).isEqualTo(ProductConditionType.DAMAGED);
        assertThat(response.returnReceipt().conditionDetail()).isEqualTo("반납 시 모서리 파손");
        assertThat(response.returnReceipt().imageUrls()).containsExactly("return-1.jpg");
        assertThat(response.returnReceipt().recordedAt()).isEqualTo(returnedAt);
    }

    @Test
    void 정상_반납_확인_결과를_거래완료_응답으로_변환한다() {
        Rental rental = rental(RentalStatus.COMPLETED);

        var response = returnMapper.toConfirmation(rental, null);

        assertThat(response.rentalId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(response.disputeId()).isNull();
    }

    @Test
    void 비정상_반납_확인_결과를_분쟁_응답으로_변환한다() {
        Rental rental = rental(RentalStatus.DISPUTED);

        var response = returnMapper.toConfirmation(rental, 50L);

        assertThat(response.rentalId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(RentalStatus.DISPUTED);
        assertThat(response.disputeId()).isEqualTo(50L);
    }

    private Rental rental(RentalStatus status) {
        return Rental.builder()
                .id(10L)
                .equipmentId(20L)
                .renterId(2L)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 10))
                .productNameSnapshot("예약 당시 맥북")
                .categorySnapshot("노트북")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(10)
                .totalPrice(BigDecimal.valueOf(300000))
                .status(status)
                .build();
    }

    private User renter() {
        return User.builder()
                .id(2L)
                .email("renter@iter.test")
                .password("encoded-password")
                .name("대여자")
                .nickname("대여자닉네임")
                .build();
    }

    private Receipt receipt(Rental rental) {
        return Receipt.builder()
                .id(30L)
                .rental(rental)
                .productCondition(ProductConditionType.NORMAL)
                .conditionDetail("수령 시 정상")
                .receivedAt(LocalDateTime.of(2026, 8, 1, 14, 30))
                .build();
    }

    private ReturnReceipt returnReceipt(Rental rental) {
        return ReturnReceipt.builder()
                .id(40L)
                .rental(rental)
                .productCondition(ProductConditionType.DAMAGED)
                .conditionDetail("반납 시 모서리 파손")
                .returnDate(LocalDate.of(2026, 8, 11))
                .build();
    }
}
