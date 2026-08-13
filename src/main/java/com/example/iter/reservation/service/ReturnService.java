package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.Dispute;
import com.example.iter.dispute.domain.repository.DisputeRepository;
import com.example.iter.reservation.domain.entity.*;
import com.example.iter.reservation.domain.repository.*;
import com.example.iter.reservation.dto.request.ReturnConfirmationRequest;
import com.example.iter.reservation.dto.response.ReturnComparisonResponse;
import com.example.iter.reservation.dto.response.ReturnConfirmationResponse;
import com.example.iter.reservation.dto.response.ReturnTargetResponse;
import com.example.iter.reservation.util.ReturnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentImageRepository equipmentImageRepository;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptImageRepository receiptImageRepository;
    private final ReturnReceiptRepository returnReceiptRepository;
    private final ReturnReceiptImageRepository returnReceiptImageRepository;
    private final DisputeRepository disputeRepository;
    private final ReturnMapper returnMapper;

    // 등록자가 최종 반납 확인을 해야 하는 거래 목록을 조회합니다.
    // 필요한 회원·반납 증빙·썸네일을 일괄 결합해 페이징 응답으로 반환
    @Transactional(readOnly = true)
    public PageResponse<ReturnTargetResponse> getReturnTargets(Long ownerId, PagingRequest request) {
        PageRequest pageable = createPageRequest(request);

        Page<Rental> rentals = rentalRepository.findReturnTargetsByOwnerIdAndStatus(
                ownerId,
                RentalStatus.RETURNED,
                pageable
        );

        if (rentals.isEmpty()) {
            return createEmptyPageResponse(rentals);
        }

        ReturnTargetData data = loadReturnTargetData(rentals.getContent());
        Page<ReturnTargetResponse> responses = rentals.map(
                rental -> toReturnTargetResponse(rental, data)
        );

        return PageResponse.from(responses);
    }


    // 거래 당사자가 수령·반납 증빙을 비교 조회합니다.
    @Transactional(readOnly = true)
    public ReturnComparisonResponse getReturnComparison(Long userId, Long rentalId) {
        Rental rental = findRental(rentalId);
        Equipment equipment = findEquipment(rental.getEquipmentId());

        validateParty(userId, rental, equipment);

        User renter = findUser(rental.getRenterId());
        Receipt receipt = findReceipt(rentalId);
        ReturnReceipt returnReceipt = findReturnReceipt(rentalId);

        List<String> receiptImageUrls = findReceiptImageUrls(receipt.getId());
        List<String> returnImageUrls = findReturnReceiptImageUrls(returnReceipt.getId());

        return returnMapper.toComparison(
                rental,
                renter,
                receipt,
                receiptImageUrls,
                returnReceipt,
                returnImageUrls
        );
    }


    // 등록자가 반납을 정상 또는 비정상으로 최종 확인합니다.
    @Transactional
    public ReturnConfirmationResponse confirmReturn(Long ownerId, Long rentalId, ReturnConfirmationRequest request) {
        Rental rental = findRentalWithLock(rentalId);
        Equipment equipment = findEquipment(rental.getEquipmentId());

        validateOwner(ownerId, equipment);
        validateConfirmationStatus(rental);
        validateEvidenceExists(rentalId);

        if (Boolean.FALSE.equals(request.hasIssue())) {
            rental.completeReturn();

            return returnMapper.toConfirmation(rental, null);
        }

        Dispute dispute = createReturnDispute(rental, ownerId, request);

        rental.openReturnDispute();

        // 장비 상태는 변경하지 않습니다.
        // 장비 등록자가 이후 장비 관리 기능에서 직접 결정합니다.

        return returnMapper.toConfirmation(
                rental,
                dispute.getId()
        );
    }

    // ===================================================

    // 반납 확인 대상 목록의 페이징과 정렬 조건을 생성합니다.
    private PageRequest createPageRequest(PagingRequest request) {
        return PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    // 조회 결과가 없을 때 빈 페이징 응답을 생성합니다.
    private PageResponse<ReturnTargetResponse> createEmptyPageResponse(Page<Rental> rentals) {
        return new PageResponse<>(
                List.of(),
                rentals.getNumber(),
                rentals.getSize(),
                rentals.getTotalElements(),
                rentals.getTotalPages()
        );
    }

    // 반납 확인 대상 응답에 필요한 회원·반납 증빙·썸네일을 일괄 조회합니다.
    private ReturnTargetData loadReturnTargetData(List<Rental> rentals) {
        Set<Long> rentalIds = rentals.stream()
                .map(Rental::getId)
                .collect(Collectors.toSet());

        Set<Long> renterIds = rentals.stream()
                .map(Rental::getRenterId)
                .collect(Collectors.toSet());

        Set<Long> equipmentIds = rentals.stream()
                .map(Rental::getEquipmentId)
                .collect(Collectors.toSet());

        return new ReturnTargetData(
                findRentersById(renterIds),
                findReturnReceiptsByRentalId(rentalIds),
                findThumbnailsByEquipmentId(equipmentIds)
        );
    }

    // 대여자 정보를 ID 기준으로 일괄 조회합니다.
    private Map<Long, User> findRentersById(Set<Long> renterIds) {
        return userRepository.findAllById(renterIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity()
                ));
    }

    // 반납 증빙을 거래 ID 기준으로 일괄 조회합니다.
    private Map<Long, ReturnReceipt> findReturnReceiptsByRentalId(Set<Long> rentalIds) {
        return returnReceiptRepository.findAllByRental_IdIn(rentalIds).stream()
                .collect(Collectors.toMap(
                        returnReceipt -> returnReceipt.getRental().getId(),
                        Function.identity()
                ));
    }

    // 장비 썸네일을 장비 ID 기준으로 일괄 조회합니다.
    private Map<Long, String> findThumbnailsByEquipmentId(Set<Long> equipmentIds) {
        return equipmentImageRepository
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(equipmentIds)
                .stream()
                .collect(Collectors.toMap(
                        image -> image.getEquipment().getId(),
                        EquipmentImage::getImageUrl,
                        (first, ignored) -> first
                ));
    }

    // 거래와 일괄 조회한 데이터를 반납 확인 대상 응답으로 변환합니다.
    private ReturnTargetResponse toReturnTargetResponse(Rental rental, ReturnTargetData data) {
        User renter = data.rentersById().get(rental.getRenterId());

        if (renter == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        ReturnReceipt returnReceipt = data.returnReceiptsByRentalId().get(rental.getId());

        if (returnReceipt == null) {
            throw new CustomException(ErrorCode.RETURN_RECEIPT_NOT_FOUND);
        }

        return returnMapper.toTarget(
                rental,
                renter,
                data.thumbnailsByEquipmentId().get(rental.getEquipmentId()),
                returnReceipt
        );
    }

    // 거래를 조회합니다.
    private Rental findRental(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    // 반납 최종 확인을 위해 거래를 비관적 쓰기 락으로 조회합니다.
    private Rental findRentalWithLock(Long rentalId) {
        return rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    // 장비를 조회합니다.
    private Equipment findEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    // 회원을 조회합니다.
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // 수령 증빙을 조회합니다.
    private Receipt findReceipt(Long rentalId) {
        return receiptRepository.findByRentalId(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECEIPT_NOT_FOUND));
    }

    // 반납 증빙을 조회합니다.
    private ReturnReceipt findReturnReceipt(Long rentalId) {
        return returnReceiptRepository.findByRentalId(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RETURN_RECEIPT_NOT_FOUND));
    }

    // 수령 증빙 이미지 URL을 등록 순서대로 조회합니다.
    private List<String> findReceiptImageUrls(Long receiptId) {
        return receiptImageRepository.findByReceipt_IdOrderBySortOrderAscIdAsc(receiptId)
                .stream()
                .map(ReceiptImage::getImageUrl)
                .toList();
    }

    // 반납 증빙 이미지 URL을 등록 순서대로 조회합니다.
    private List<String> findReturnReceiptImageUrls(Long returnReceiptId) {
        return returnReceiptImageRepository
                .findByReturnReceipt_IdOrderBySortOrderAscIdAsc(returnReceiptId)
                .stream()
                .map(ReturnReceiptImage::getImageUrl)
                .toList();
    }

    // 반납 최종 확인에 필요한 수령·반납 증빙이 존재하는지 검사합니다.
    private void validateEvidenceExists(Long rentalId) {
        findReceipt(rentalId);
        findReturnReceipt(rentalId);
    }

    // 비정상 반납에 대한 최소 분쟁을 생성합니다.
    private Dispute createReturnDispute(
            Rental rental,
            Long ownerId,
            ReturnConfirmationRequest request
    ) {
        return disputeRepository.save(
                Dispute.builder()
                        .rentalId(rental.getId())
                        .reporterId(ownerId)
                        .respondentId(rental.getRenterId())
                        .reason(request.disputeReason().trim())
                        .description(request.disputeDescription().trim())
                        .build()
        );
    }

    private void validateParty(Long userId, Rental rental, Equipment equipment) {
        boolean renter = rental.isRenter(userId);
        boolean owner = equipment.isOwnedBy(userId);

        if (!renter && !owner) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 로그인 사용자가 장비 등록자인지 확인합니다.
    private void validateOwner(Long ownerId, Equipment equipment) {
        if (!equipment.isOwnedBy(ownerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 반납 최종 확인이 가능한 거래 상태인지 검사합니다.
    private void validateConfirmationStatus(Rental rental) {
        if (rental.getStatus() == RentalStatus.COMPLETED || rental.getStatus() == RentalStatus.DISPUTED) {
            throw new CustomException(ErrorCode.RETURN_ALREADY_CONFIRMED);
        }

        if (rental.getStatus() != RentalStatus.RETURNED) {
            throw new CustomException(ErrorCode.INVALID_RETURN_CONFIRMATION_STATUS);
        }
    }

    // 반납 확인 대상 목록 응답 생성에 필요한 일괄 조회 결과입니다.
    private record ReturnTargetData(
            Map<Long, User> rentersById,
            Map<Long, ReturnReceipt> returnReceiptsByRentalId,
            Map<Long, String> thumbnailsByEquipmentId
    ) {
    }
}
