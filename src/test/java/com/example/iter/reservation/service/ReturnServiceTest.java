package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.Dispute;
import com.example.iter.dispute.domain.repository.DisputeRepository;
import com.example.iter.reservation.domain.entity.ProductConditionType;
import com.example.iter.reservation.domain.entity.Receipt;
import com.example.iter.reservation.domain.entity.ReceiptImage;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.entity.ReturnReceipt;
import com.example.iter.reservation.domain.entity.ReturnReceiptImage;
import com.example.iter.reservation.domain.repository.ReceiptImageRepository;
import com.example.iter.reservation.domain.repository.ReceiptRepository;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.domain.repository.ReturnReceiptImageRepository;
import com.example.iter.reservation.domain.repository.ReturnReceiptRepository;
import com.example.iter.reservation.dto.request.ReturnConfirmationRequest;
import com.example.iter.reservation.util.ReturnMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long RENTER_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;
    private static final Long RENTAL_ID = 10L;
    private static final Long EQUIPMENT_ID = 20L;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentImageRepository equipmentImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptImageRepository receiptImageRepository;

    @Mock
    private ReturnReceiptRepository returnReceiptRepository;

    @Mock
    private ReturnReceiptImageRepository returnReceiptImageRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Spy
    private ReturnMapper returnMapper = new ReturnMapper();

    @InjectMocks
    private ReturnService returnService;

    @Test
    void 반납_확인_대상이_없으면_빈_페이지를_반환한다() {
        when(rentalRepository.findReturnTargetsByOwnerIdAndStatus(
                eq(OWNER_ID),
                eq(RentalStatus.RETURNED),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = returnService.getReturnTargets(OWNER_ID, new PagingRequest(null, null));

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        verifyNoInteractions(userRepository, returnReceiptRepository, equipmentImageRepository, returnMapper);
    }

    @Test
    void 반납_확인_대상을_회원_증빙_썸네일과_함께_일괄_조회한다() {
        Rental rental = rental(RentalStatus.RETURNED);
        User renter = renter();
        ReturnReceipt returnReceipt = returnReceipt(rental);
        Equipment equipment = equipment();
        EquipmentImage thumbnail = EquipmentImage.builder()
                .id(50L)
                .equipment(equipment)
                .imageUrl("https://example.com/thumbnail.jpg")
                .sortOrder(1)
                .thumbnail(true)
                .build();

        when(rentalRepository.findReturnTargetsByOwnerIdAndStatus(
                eq(OWNER_ID),
                eq(RentalStatus.RETURNED),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(rental), PageRequest.of(0, 20), 1));
        when(userRepository.findAllById(any())).thenReturn(List.of(renter));
        when(returnReceiptRepository.findAllByRental_IdIn(any())).thenReturn(List.of(returnReceipt));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of(thumbnail));

        var response = returnService.getReturnTargets(OWNER_ID, new PagingRequest(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().rentalId()).isEqualTo(RENTAL_ID);
        assertThat(response.content().getFirst().equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(response.content().getFirst().renter().userId()).isEqualTo(RENTER_ID);
        assertThat(response.content().getFirst().thumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
        assertThat(response.content().getFirst().returnDate()).isEqualTo(LocalDate.of(2026, 8, 10));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(rentalRepository).findReturnTargetsByOwnerIdAndStatus(
                eq(OWNER_ID),
                eq(RentalStatus.RETURNED),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("updatedAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void 반납_확인_대상의_대여자가_없으면_오류를_반환한다() {
        Rental rental = rental(RentalStatus.RETURNED);
        when(rentalRepository.findReturnTargetsByOwnerIdAndStatus(eq(OWNER_ID), eq(RentalStatus.RETURNED), any()))
                .thenReturn(new PageImpl<>(List.of(rental)));
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(returnReceiptRepository.findAllByRental_IdIn(any())).thenReturn(List.of(returnReceipt(rental)));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> returnService.getReturnTargets(OWNER_ID, new PagingRequest(0, 20)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 거래_대여자는_수령과_반납_증빙을_비교_조회할_수_있다() {
        Rental rental = rental(RentalStatus.RETURNED);
        Receipt receipt = receipt(rental);
        ReturnReceipt returnReceipt = returnReceipt(rental);
        stubComparisonData(rental, receipt, returnReceipt);
        when(receiptImageRepository.findByReceipt_IdOrderBySortOrderAscIdAsc(30L))
                .thenReturn(List.of(
                        ReceiptImage.builder().id(31L).receipt(receipt).imageUrl("receipt-1.jpg").sortOrder(1).build(),
                        ReceiptImage.builder().id(32L).receipt(receipt).imageUrl("receipt-2.jpg").sortOrder(2).build()
                ));
        when(returnReceiptImageRepository.findByReturnReceipt_IdOrderBySortOrderAscIdAsc(40L))
                .thenReturn(List.of(
                        ReturnReceiptImage.builder().id(41L).returnReceipt(returnReceipt).imageUrl("return-1.jpg").sortOrder(1).build()
                ));

        var response = returnService.getReturnComparison(RENTER_ID, RENTAL_ID);

        assertThat(response.rentalId()).isEqualTo(RENTAL_ID);
        assertThat(response.equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(response.receipt().productCondition()).isEqualTo(ProductConditionType.NORMAL);
        assertThat(response.receipt().imageUrls()).containsExactly("receipt-1.jpg", "receipt-2.jpg");
        assertThat(response.returnReceipt().productCondition()).isEqualTo(ProductConditionType.DAMAGED);
        assertThat(response.returnReceipt().imageUrls()).containsExactly("return-1.jpg");
        assertThat(response.receipt().recordedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 14, 30));
    }

    @Test
    void 거래_제3자는_수령과_반납_증빙을_조회할_수_없다() {
        Rental rental = rental(RentalStatus.RETURNED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

        assertThatThrownBy(() -> returnService.getReturnComparison(OUTSIDER_ID, RENTAL_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(receiptRepository, returnReceiptRepository);
    }

    @Test
    void 수령_증빙이_없으면_비교_조회할_수_없다() {
        Rental rental = rental(RentalStatus.RETURNED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(userRepository.findById(RENTER_ID)).thenReturn(Optional.of(renter()));
        when(receiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> returnService.getReturnComparison(OWNER_ID, RENTAL_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
    }

    @Test
    void 정상_반납을_확인하면_거래를_COMPLETED로_변경한다() {
        Rental rental = rental(RentalStatus.RETURNED);
        stubConfirmationData(rental);

        var response = returnService.confirmReturn(
                OWNER_ID,
                RENTAL_ID,
                new ReturnConfirmationRequest(false, null, null)
        );

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(response.status()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(response.disputeId()).isNull();
        verify(rentalRepository).findWithLockById(RENTAL_ID);
        verifyNoInteractions(disputeRepository);
    }

    @Test
    void 비정상_반납을_확인하면_분쟁을_생성하고_거래를_DISPUTED로_변경한다() {
        Rental rental = rental(RentalStatus.RETURNED);
        stubConfirmationData(rental);
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute value = invocation.getArgument(0);
            return Dispute.builder()
                    .id(50L)
                    .rentalId(value.getRentalId())
                    .reporterId(value.getReporterId())
                    .respondentId(value.getRespondentId())
                    .reason(value.getReason())
                    .description(value.getDescription())
                    .build();
        });

        var response = returnService.confirmReturn(
                OWNER_ID,
                RENTAL_ID,
                new ReturnConfirmationRequest(true, "  모서리 파손  ", "  반납 시 파손을 확인했습니다.  ")
        );

        ArgumentCaptor<Dispute> disputeCaptor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeRepository).save(disputeCaptor.capture());
        Dispute savedDispute = disputeCaptor.getValue();

        assertThat(savedDispute.getRentalId()).isEqualTo(RENTAL_ID);
        assertThat(savedDispute.getReporterId()).isEqualTo(OWNER_ID);
        assertThat(savedDispute.getRespondentId()).isEqualTo(RENTER_ID);
        assertThat(savedDispute.getReason()).isEqualTo("모서리 파손");
        assertThat(savedDispute.getDescription()).isEqualTo("반납 시 파손을 확인했습니다.");
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.DISPUTED);
        assertThat(response.status()).isEqualTo(RentalStatus.DISPUTED);
        assertThat(response.disputeId()).isEqualTo(50L);
    }

    @Test
    void 장비_등록자가_아니면_반납을_최종_확인할_수_없다() {
        Rental rental = rental(RentalStatus.RETURNED);
        when(rentalRepository.findWithLockById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

        assertThatThrownBy(() -> returnService.confirmReturn(
                OUTSIDER_ID,
                RENTAL_ID,
                new ReturnConfirmationRequest(false, null, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(receiptRepository, returnReceiptRepository, disputeRepository);
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
    }

    @Test
    void 이미_완료되었거나_분쟁중인_반납은_다시_확인할_수_없다() {
        for (RentalStatus status : List.of(RentalStatus.COMPLETED, RentalStatus.DISPUTED)) {
            Rental rental = rental(status);
            when(rentalRepository.findWithLockById(RENTAL_ID)).thenReturn(Optional.of(rental));
            when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

            assertThatThrownBy(() -> returnService.confirmReturn(
                    OWNER_ID,
                    RENTAL_ID,
                    new ReturnConfirmationRequest(false, null, null)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RETURN_ALREADY_CONFIRMED);
        }
    }

    @Test
    void RETURNED_상태가_아니면_반납을_최종_확인할_수_없다() {
        Rental rental = rental(RentalStatus.RETURNING);
        when(rentalRepository.findWithLockById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

        assertThatThrownBy(() -> returnService.confirmReturn(
                OWNER_ID,
                RENTAL_ID,
                new ReturnConfirmationRequest(false, null, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_RETURN_CONFIRMATION_STATUS);
    }

    @Test
    void 반납_증빙이_없으면_상태를_변경하지_않는다() {
        Rental rental = rental(RentalStatus.RETURNED);
        when(rentalRepository.findWithLockById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(receiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.of(receipt(rental)));
        when(returnReceiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> returnService.confirmReturn(
                OWNER_ID,
                RENTAL_ID,
                new ReturnConfirmationRequest(true, "파손", "파손 확인")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_RECEIPT_NOT_FOUND);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        verifyNoInteractions(disputeRepository);
    }

    private void stubComparisonData(Rental rental, Receipt receipt, ReturnReceipt returnReceipt) {
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(userRepository.findById(RENTER_ID)).thenReturn(Optional.of(renter()));
        when(receiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.of(receipt));
        when(returnReceiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.of(returnReceipt));
    }

    private void stubConfirmationData(Rental rental) {
        when(rentalRepository.findWithLockById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(receiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.of(receipt(rental)));
        when(returnReceiptRepository.findByRentalId(RENTAL_ID)).thenReturn(Optional.of(returnReceipt(rental)));
    }

    private Rental rental(RentalStatus status) {
        return Rental.builder()
                .id(RENTAL_ID)
                .equipmentId(EQUIPMENT_ID)
                .renterId(RENTER_ID)
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

    private Equipment equipment() {
        return Equipment.builder()
                .id(EQUIPMENT_ID)
                .ownerId(OWNER_ID)
                .category("노트북")
                .name("현재 장비명")
                .dailyPrice(BigDecimal.valueOf(50000))
                .build();
    }

    private User renter() {
        return User.builder()
                .id(RENTER_ID)
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
                .returnDate(LocalDate.of(2026, 8, 10))
                .build();
    }
}
