package com.example.iter.reservation.domain.repository;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.ProductConditionType;
import com.example.iter.reservation.domain.entity.Receipt;
import com.example.iter.reservation.domain.entity.ReceiptImage;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.entity.ReturnReceipt;
import com.example.iter.reservation.domain.entity.ReturnReceiptImage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ReturnRepositoryTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Long RENTER_ID = 3L;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentImageRepository equipmentImageRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptImageRepository receiptImageRepository;

    @Autowired
    private ReturnReceiptRepository returnReceiptRepository;

    @Autowired
    private ReturnReceiptImageRepository returnReceiptImageRepository;

    @Test
    void 등록자_소유_장비의_RETURNED_거래만_반납_확인_대상으로_조회한다() {
        Equipment ownerEquipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        Equipment otherEquipment = equipmentRepository.saveAndFlush(equipment(OTHER_OWNER_ID, "다른 장비"));

        Rental returnedOld = rentalRepository.saveAndFlush(rental(ownerEquipment, RentalStatus.RETURNED, "반납 1"));
        Rental returnedNew = rentalRepository.saveAndFlush(rental(ownerEquipment, RentalStatus.RETURNED, "반납 2"));
        rentalRepository.saveAndFlush(rental(ownerEquipment, RentalStatus.COMPLETED, "완료 거래"));
        rentalRepository.saveAndFlush(rental(otherEquipment, RentalStatus.RETURNED, "다른 등록자 거래"));

        var result = rentalRepository.findReturnTargetsByOwnerIdAndStatus(
                OWNER_ID,
                RentalStatus.RETURNED,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
                )
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(returnedNew.getId(), returnedOld.getId());
    }

    @Test
    void 반납_확인_대상을_페이지_크기에_맞게_조회한다() {
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        rentalRepository.saveAndFlush(rental(equipment, RentalStatus.RETURNED, "반납 1"));
        rentalRepository.saveAndFlush(rental(equipment, RentalStatus.RETURNED, "반납 2"));
        rentalRepository.saveAndFlush(rental(equipment, RentalStatus.RETURNED, "반납 3"));

        var result = rentalRepository.findReturnTargetsByOwnerIdAndStatus(
                OWNER_ID,
                RentalStatus.RETURNED,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void 거래를_비관적_쓰기_락으로_조회한다() {
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        Rental savedRental = rentalRepository.saveAndFlush(
                rental(equipment, RentalStatus.RETURNED, "반납 거래")
        );
        entityManager.clear();

        Rental lockedRental = rentalRepository.findWithLockById(savedRental.getId()).orElseThrow();

        assertThat(entityManager.getLockMode(lockedRental)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 거래_ID로_수령과_반납_증빙을_조회한다() {
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        Rental rental = rentalRepository.saveAndFlush(
                rental(equipment, RentalStatus.RETURNED, "반납 거래")
        );
        Receipt receipt = receiptRepository.saveAndFlush(receipt(rental));
        ReturnReceipt returnReceipt = returnReceiptRepository.saveAndFlush(returnReceipt(rental));

        assertThat(receiptRepository.findByRentalId(rental.getId()))
                .get()
                .extracting(Receipt::getId)
                .isEqualTo(receipt.getId());
        assertThat(returnReceiptRepository.findByRentalId(rental.getId()))
                .get()
                .extracting(ReturnReceipt::getId)
                .isEqualTo(returnReceipt.getId());
    }

    @Test
    void 여러_거래의_반납_증빙을_한번에_조회한다() {
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        Rental firstRental = rentalRepository.saveAndFlush(
                rental(equipment, RentalStatus.RETURNED, "반납 1")
        );
        Rental secondRental = rentalRepository.saveAndFlush(
                rental(equipment, RentalStatus.RETURNED, "반납 2")
        );
        ReturnReceipt firstReceipt = returnReceiptRepository.saveAndFlush(returnReceipt(firstRental));
        ReturnReceipt secondReceipt = returnReceiptRepository.saveAndFlush(returnReceipt(secondRental));

        var result = returnReceiptRepository.findAllByRental_IdIn(
                Set.of(firstRental.getId(), secondRental.getId())
        );

        assertThat(result)
                .extracting(ReturnReceipt::getId)
                .containsExactlyInAnyOrder(firstReceipt.getId(), secondReceipt.getId());
    }

    @Test
    void 수령_증빙_이미지를_정렬순서와_ID_순으로_조회한다() {
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        Rental rental = rentalRepository.saveAndFlush(
                rental(equipment, RentalStatus.RETURNED, "반납 거래")
        );
        Receipt receipt = receiptRepository.saveAndFlush(receipt(rental));
        receiptImageRepository.saveAndFlush(receiptImage(receipt, "first-a.jpg", 1));
        receiptImageRepository.saveAndFlush(receiptImage(receipt, "second.jpg", 2));
        receiptImageRepository.saveAndFlush(receiptImage(receipt, "first-b.jpg", 1));

        var result = receiptImageRepository.findByReceipt_IdOrderBySortOrderAscIdAsc(receipt.getId());

        assertThat(result)
                .extracting(ReceiptImage::getImageUrl)
                .containsExactly("first-a.jpg", "first-b.jpg", "second.jpg");
    }

    @Test
    void 반납_증빙_이미지를_정렬순서와_ID_순으로_조회한다() {
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "소유 장비"));
        Rental rental = rentalRepository.saveAndFlush(
                rental(equipment, RentalStatus.RETURNED, "반납 거래")
        );
        ReturnReceipt returnReceipt = returnReceiptRepository.saveAndFlush(returnReceipt(rental));
        returnReceiptImageRepository.saveAndFlush(returnReceiptImage(returnReceipt, "first-a.jpg", 1));
        returnReceiptImageRepository.saveAndFlush(returnReceiptImage(returnReceipt, "second.jpg", 2));
        returnReceiptImageRepository.saveAndFlush(returnReceiptImage(returnReceipt, "first-b.jpg", 1));

        var result = returnReceiptImageRepository
                .findByReturnReceipt_IdOrderBySortOrderAscIdAsc(returnReceipt.getId());

        assertThat(result)
                .extracting(ReturnReceiptImage::getImageUrl)
                .containsExactly("first-a.jpg", "first-b.jpg", "second.jpg");
    }

    @Test
    void 여러_장비에서_썸네일만_정렬해_조회한다() {
        Equipment firstEquipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "첫 장비"));
        Equipment secondEquipment = equipmentRepository.saveAndFlush(equipment(OWNER_ID, "두 번째 장비"));
        equipmentImageRepository.saveAndFlush(equipmentImage(firstEquipment, "normal.jpg", 0, false));
        equipmentImageRepository.saveAndFlush(equipmentImage(firstEquipment, "first-thumbnail.jpg", 2, true));
        equipmentImageRepository.saveAndFlush(equipmentImage(secondEquipment, "second-thumbnail.jpg", 1, true));

        var result = equipmentImageRepository
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(
                        Set.of(firstEquipment.getId(), secondEquipment.getId())
                );

        assertThat(result)
                .extracting(EquipmentImage::getImageUrl)
                .containsExactly("second-thumbnail.jpg", "first-thumbnail.jpg");
    }

    private Equipment equipment(Long ownerId, String name) {
        return Equipment.builder()
                .ownerId(ownerId)
                .category(EquipmentCategory.LAPTOP)
                .name(name)
                .dailyPrice(BigDecimal.valueOf(30000))
                .build();
    }

    private Rental rental(Equipment equipment, RentalStatus status, String snapshotName) {
        return Rental.builder()
                .equipmentId(equipment.getId())
                .renterId(RENTER_ID)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 10))
                .productNameSnapshot(snapshotName)
                .categorySnapshot("노트북")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(10)
                .totalPrice(BigDecimal.valueOf(300000))
                .status(status)
                .build();
    }

    private Receipt receipt(Rental rental) {
        return Receipt.builder()
                .rental(rental)
                .productCondition(ProductConditionType.NORMAL)
                .conditionDetail("수령 시 정상")
                .receivedAt(LocalDateTime.of(2026, 8, 1, 14, 30))
                .build();
    }

    private ReturnReceipt returnReceipt(Rental rental) {
        return ReturnReceipt.builder()
                .rental(rental)
                .productCondition(ProductConditionType.DAMAGED)
                .conditionDetail("반납 시 모서리 파손")
                .returnDate(LocalDate.of(2026, 8, 11))
                .build();
    }

    private ReceiptImage receiptImage(Receipt receipt, String imageUrl, int sortOrder) {
        return ReceiptImage.builder()
                .receipt(receipt)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }

    private ReturnReceiptImage returnReceiptImage(
            ReturnReceipt returnReceipt,
            String imageUrl,
            int sortOrder
    ) {
        return ReturnReceiptImage.builder()
                .returnReceipt(returnReceipt)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }

    private EquipmentImage equipmentImage(
            Equipment equipment,
            String imageUrl,
            int sortOrder,
            boolean thumbnail
    ) {
        return EquipmentImage.builder()
                .equipment(equipment)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .build();
    }
}
