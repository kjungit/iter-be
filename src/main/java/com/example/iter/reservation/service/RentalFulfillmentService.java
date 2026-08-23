package com.example.iter.reservation.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.delivery.domain.entity.Shipping;
import com.example.iter.delivery.domain.entity.ShippingStatus;
import com.example.iter.delivery.domain.entity.ShippingType;
import com.example.iter.delivery.domain.repository.ShippingRepository;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.*;
import com.example.iter.reservation.domain.repository.*;
import com.example.iter.reservation.dto.request.ReceiptCreateRequest;
import com.example.iter.reservation.dto.request.ReturnEvidenceCreateRequest;
import com.example.iter.reservation.dto.request.ShippingRegisterRequest;
import com.example.iter.reservation.dto.response.ReceiptCreateResponse;
import com.example.iter.reservation.dto.response.ReturnEvidenceCreateResponse;
import com.example.iter.reservation.dto.response.ReturnRequestResponse;
import com.example.iter.reservation.dto.response.ShippingRegisterResponse;
import com.example.iter.reservation.event.RentalReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

// 대여 상태머신 중 "승인 이후 ~ 반납 도착"(APPROVED->SHIPPING->RENTING->RETURN_REQUESTED->RETURNED)
// 구간을 다룬다. RETURNED->COMPLETED/DISPUTED 최종 확인은 ReturnService(기존 구현)가 담당.
//
// RECEIVED/RETURNING은 ERD상 별도 상태로 정의돼 있지만, 이 서비스는 "수령확인 = 대여 시작"
// "반납증빙 제출 = 반납 도착"으로 한 요청 안에서 곧바로 다음 상태까지 묶어 처리한다 — 프론트
// UX가 그 중간 상태를 별도 액션으로 노출하지 않기 때문 (RentalStatus enum 값 자체는 그대로 유지).
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RentalFulfillmentService {

    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final ShippingRepository shippingRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptImageRepository receiptImageRepository;
    private final ReturnReceiptRepository returnReceiptRepository;
    private final ReturnReceiptImageRepository returnReceiptImageRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 장비 등록자가 출고 배송 정보를 등록합니다. APPROVED -> SHIPPING
    // mock 배송이라 실제 택배사 연동/배송 추적이 없다 — 등록 즉시 배송완료로 기록한다.
    public ShippingRegisterResponse registerShipping(Long ownerId, Long rentalId, ShippingRegisterRequest request) {
        Rental rental = findRental(rentalId);
        Equipment equipment = findEquipment(rental.getEquipmentId());
        validateOwner(ownerId, equipment);

        if (rental.getStatus() != RentalStatus.APPROVED) {
            throw new CustomException(ErrorCode.RENTAL_NOT_SHIPPABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        shippingRepository.save(Shipping.builder()
                .rentalId(rental.getId())
                .type(ShippingType.OUTBOUND)
                .carrier(request.carrier())
                .trackingNumber(request.trackingNumber())
                .status(ShippingStatus.DELIVERED)
                .shippedAt(now)
                .deliveredAt(now)
                .build());

        rental.changeStatus(RentalStatus.SHIPPING);
        log.info("대여 출고 처리: rentalId={}, equipmentId={}, ownerId={}, status={}",
                rentalId, rental.getEquipmentId(), ownerId, rental.getStatus());

        return new ShippingRegisterResponse(
                rental.getId(), rental.getStatus(), request.carrier(), request.trackingNumber(), now);
    }

    // 대여자가 수령 증빙을 제출합니다. SHIPPING -> RENTING (수령확인 = 대여 시작)
    public ReceiptCreateResponse createReceipt(Long renterId, Long rentalId, ReceiptCreateRequest request) {
        Rental rental = findRental(rentalId);
        validateRenter(renterId, rental);

        if (rental.getStatus() != RentalStatus.SHIPPING) {
            throw new CustomException(ErrorCode.RENTAL_NOT_RECEIVABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .rental(rental)
                .productCondition(request.productCondition())
                .conditionDetail(request.conditionDetail())
                .receivedAt(now)
                .build());
        saveReceiptImages(receipt, request.imageUrls());

        rental.changeStatus(RentalStatus.RENTING);
        eventPublisher.publishEvent(new RentalReceivedEvent(rental.getId()));
        log.info("대여 수령 처리: rentalId={}, renterId={}, status={}",
                rentalId, renterId, rental.getStatus());

        return new ReceiptCreateResponse(rental.getId(), rental.getStatus(), now);
    }

    // 대여자가 반납을 신청합니다. RENTING -> RETURN_REQUESTED
    public ReturnRequestResponse requestReturn(Long renterId, Long rentalId) {
        Rental rental = findRental(rentalId);
        validateRenter(renterId, rental);

        if (rental.getStatus() != RentalStatus.RENTING) {
            throw new CustomException(ErrorCode.RENTAL_NOT_RETURN_REQUESTABLE);
        }

        rental.changeStatus(RentalStatus.RETURN_REQUESTED);
        log.info("대여 반납 신청 처리: rentalId={}, renterId={}, status={}",
                rentalId, renterId, rental.getStatus());
        return new ReturnRequestResponse(rental.getId(), rental.getStatus());
    }

    // 대여자가 반납 증빙을 제출합니다. RETURN_REQUESTED -> RETURNED (증빙 제출 = 반납 도착)
    // 이후 등록자의 최종 확인(ReturnService.confirmReturn)이 RETURNED -> COMPLETED/DISPUTED를 처리한다.
    public ReturnEvidenceCreateResponse createReturnEvidence(
            Long renterId, Long rentalId, ReturnEvidenceCreateRequest request) {
        Rental rental = findRental(rentalId);
        validateRenter(renterId, rental);

        if (rental.getStatus() != RentalStatus.RETURN_REQUESTED) {
            throw new CustomException(ErrorCode.RENTAL_NOT_RETURN_EVIDENCE_SUBMITTABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        ReturnReceipt returnReceipt = returnReceiptRepository.save(ReturnReceipt.builder()
                .rental(rental)
                .productCondition(request.productCondition())
                .conditionDetail(request.conditionDetail())
                .returnDate(today)
                .build());
        saveReturnReceiptImages(returnReceipt, request.imageUrls());

        shippingRepository.save(Shipping.builder()
                .rentalId(rental.getId())
                .type(ShippingType.RETURN)
                .status(ShippingStatus.DELIVERED)
                .shippedAt(now)
                .deliveredAt(now)
                .build());

        rental.changeStatus(RentalStatus.RETURNED);
        log.info("대여 반납 증빙 등록 처리: rentalId={}, renterId={}, status={}",
                rentalId, renterId, rental.getStatus());

        return new ReturnEvidenceCreateResponse(rental.getId(), rental.getStatus(), today);
    }

    private void saveReceiptImages(Receipt receipt, List<String> imageUrls) {
        List<ReceiptImage> images = IntStream.range(0, imageUrls.size())
                .mapToObj(index -> ReceiptImage.builder()
                        .receipt(receipt)
                        .imageUrl(imageUrls.get(index))
                        .sortOrder(index)
                        .build())
                .toList();
        receiptImageRepository.saveAll(images);
    }

    private void saveReturnReceiptImages(ReturnReceipt returnReceipt, List<String> imageUrls) {
        List<ReturnReceiptImage> images = IntStream.range(0, imageUrls.size())
                .mapToObj(index -> ReturnReceiptImage.builder()
                        .returnReceipt(returnReceipt)
                        .imageUrl(imageUrls.get(index))
                        .sortOrder(index)
                        .build())
                .toList();
        returnReceiptImageRepository.saveAll(images);
    }

    private Rental findRental(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    private Equipment findEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    private void validateOwner(Long ownerId, Equipment equipment) {
        if (!equipment.isOwnedBy(ownerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateRenter(Long renterId, Rental rental) {
        if (!rental.isRenter(renterId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
