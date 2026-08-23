package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalHistoryRepository;
import com.example.iter.reservation.dto.request.RentalHistorySearchRequest;
import com.example.iter.reservation.util.RentalHistoryMapper;
import com.example.iter.reservation.util.RentalOverduePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalHistoryServiceTest {

    private static final Long RENTER_ID = 1L;
    private static final Long OWNER_ID = 2L;
    private static final Long EQUIPMENT_ID = 10L;

    @Mock
    private RentalHistoryRepository rentalHistoryRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentImageRepository equipmentImageRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private RentalHistoryMapper rentalHistoryMapper = new RentalHistoryMapper();

    @InjectMocks
    private RentalHistoryService rentalHistoryService;

    @Test
    void 빌린_장비_이력은_예약_스냅샷과_장비_등록자를_반환한다() {
        LocalDate today = LocalDate.now();
        Rental rental = rental(
                100L,
                EQUIPMENT_ID,
                RENTER_ID,
                RentalStatus.RENTING,
                today.minusDays(3),
                "예약 당시 맥북"
        );
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, "현재 변경된 장비명");
        User owner = user(OWNER_ID, "등록자");
        EquipmentImage thumbnail = thumbnail(1L, equipment, "https://example.com/macbook.jpg", 1);

        when(rentalHistoryRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page(rental));
        when(equipmentRepository.findAllById(any())).thenReturn(List.of(equipment));
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of(thumbnail));

        var response = rentalHistoryService.getBorrowedHistory(
                RENTER_ID,
                new RentalHistorySearchRequest(RentalStatus.RENTING, "  맥북  ", 0, 20)
        );

        assertThat(response.content()).hasSize(1);
        var history = response.content().getFirst();
        assertThat(history.rentalId()).isEqualTo(100L);
        assertThat(history.equipmentId()).isEqualTo(EQUIPMENT_ID);
        assertThat(history.equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(history.equipmentName()).isNotEqualTo(equipment.getName());
        assertThat(history.thumbnailUrl()).isEqualTo("https://example.com/macbook.jpg");
        assertThat(history.counterparty().userId()).isEqualTo(OWNER_ID);
        assertThat(history.counterparty().nickName()).isEqualTo("등록자");
        assertThat(history.totalPrice()).isEqualByComparingTo("30000");
        assertThat(history.overdueDays()).isEqualTo(3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(rentalHistoryRepository).findAll(
                any(Specification.class),
                pageableCaptor.capture()
        );
        assertThat(sortDescription(pageableCaptor.getValue()))
                .containsExactly("createdAt: DESC", "id: DESC");
    }

    @Test
    void 빌려준_장비_이력은_대여자를_상대방으로_반환한다() {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, "카메라");
        Rental rental = rental(
                101L,
                EQUIPMENT_ID,
                RENTER_ID,
                RentalStatus.COMPLETED,
                LocalDate.now().minusDays(10),
                "예약 당시 카메라"
        );
        User renter = user(RENTER_ID, "대여자");

        when(rentalHistoryRepository.findLentHistory(
                eq(OWNER_ID),
                eq(RentalStatus.COMPLETED),
                eq("카메라"),
                any(Pageable.class)
        )).thenReturn(page(rental));
        when(userRepository.findAllById(any())).thenReturn(List.of(renter));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of(thumbnail(2L, equipment, "https://example.com/camera.jpg", 1)));

        var response = rentalHistoryService.getLentHistory(
                OWNER_ID,
                new RentalHistorySearchRequest(RentalStatus.COMPLETED, "카메라", 0, 20)
        );

        assertThat(response.content()).singleElement().satisfies(history -> {
            assertThat(history.counterparty().userId()).isEqualTo(RENTER_ID);
            assertThat(history.counterparty().nickName()).isEqualTo("대여자");
            assertThat(history.equipmentName()).isEqualTo("예약 당시 카메라");
            assertThat(history.thumbnailUrl()).isEqualTo("https://example.com/camera.jpg");
            assertThat(history.overdueDays()).isZero();
        });
        verify(equipmentRepository, never()).findAllById(any());
    }

    @Test
    void 빈_검색어는_null로_정규화하고_빈_페이지는_추가_조회하지_않는다() {
        Page<Rental> emptyPage = Page.empty(PageRequest.of(2, 5));
        when(rentalHistoryRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        var response = rentalHistoryService.getBorrowedHistory(
                RENTER_ID,
                new RentalHistorySearchRequest(null, "   ", 2, 5)
        );

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        verifyNoInteractions(equipmentRepository, equipmentImageRepository, userRepository);
        verify(rentalHistoryMapper, never()).toResponse(any(), any(), any(), any(Integer.class));
    }

    @Test
    void 빌린_이력의_장비가_없으면_예외가_발생한다() {
        Rental rental = rental(
                102L,
                EQUIPMENT_ID,
                RENTER_ID,
                RentalStatus.COMPLETED,
                LocalDate.now(),
                "삭제된 장비"
        );
        when(rentalHistoryRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page(rental));
        when(equipmentRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> rentalHistoryService.getBorrowedHistory(
                RENTER_ID,
                new RentalHistorySearchRequest(null, null, 0, 20)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_FOUND);

        verifyNoInteractions(userRepository, equipmentImageRepository);
    }

    @Test
    void 빌린_이력의_장비_등록자가_없으면_예외가_발생한다() {
        Rental rental = rental(
                103L,
                EQUIPMENT_ID,
                RENTER_ID,
                RentalStatus.COMPLETED,
                LocalDate.now(),
                "장비"
        );
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, "장비");
        when(rentalHistoryRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page(rental));
        when(equipmentRepository.findAllById(any())).thenReturn(List.of(equipment));
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> rentalHistoryService.getBorrowedHistory(
                RENTER_ID,
                new RentalHistorySearchRequest(null, null, 0, 20)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 빌려준_이력의_대여자가_없으면_예외가_발생한다() {
        Rental rental = rental(
                104L,
                EQUIPMENT_ID,
                RENTER_ID,
                RentalStatus.COMPLETED,
                LocalDate.now(),
                "장비"
        );
        when(rentalHistoryRepository.findLentHistory(
                eq(OWNER_ID),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page(rental));
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> rentalHistoryService.getLentHistory(
                OWNER_ID,
                new RentalHistorySearchRequest(null, null, 0, 20)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 빌린_장비_연체_이력은_연체_상태와_종료일_순으로_조회한다() {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, "연체 장비");
        User owner = user(OWNER_ID, "등록자");

        when(rentalHistoryRepository.findByRenterIdAndEndDateBeforeAndStatusIn(
                eq(RENTER_ID),
                any(LocalDate.class),
                eq(RentalOverduePolicy.statuses()),
                any(Pageable.class)
        )).thenAnswer(invocation -> {
            LocalDate today = invocation.getArgument(1);
            Pageable pageable = invocation.getArgument(3);
            Rental overdueRental = rental(
                    105L,
                    EQUIPMENT_ID,
                    RENTER_ID,
                    RentalStatus.RETURNING,
                    today.minusDays(4),
                    "연체 장비"
            );
            return new PageImpl<>(List.of(overdueRental), pageable, 1);
        });
        when(equipmentRepository.findAllById(any())).thenReturn(List.of(equipment));
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of());

        var response = rentalHistoryService.getBorrowedOverdueHistory(
                RENTER_ID,
                new PagingRequest(0, 20)
        );

        assertThat(response.content()).singleElement()
                .extracting(history -> history.overdueDays())
                .isEqualTo(4);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(rentalHistoryRepository).findByRenterIdAndEndDateBeforeAndStatusIn(
                eq(RENTER_ID),
                dateCaptor.capture(),
                eq(RentalOverduePolicy.statuses()),
                pageableCaptor.capture()
        );
        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now());
        assertThat(sortDescription(pageableCaptor.getValue()))
                .containsExactly("endDate: ASC", "createdAt: DESC", "id: DESC");
    }

    @Test
    void 빌려준_장비_연체_이력은_등록자_ID로_조회한다() {
        User renter = user(RENTER_ID, "대여자");
        when(rentalHistoryRepository.findLentOverdueHistory(
                eq(OWNER_ID),
                any(LocalDate.class),
                eq(RentalOverduePolicy.statuses()),
                any(Pageable.class)
        )).thenAnswer(invocation -> {
            LocalDate today = invocation.getArgument(1);
            Pageable pageable = invocation.getArgument(3);
            Rental overdueRental = rental(
                    106L,
                    EQUIPMENT_ID,
                    RENTER_ID,
                    RentalStatus.RENTING,
                    today.minusDays(2),
                    "연체 장비"
            );
            return new PageImpl<>(List.of(overdueRental), pageable, 1);
        });
        when(userRepository.findAllById(any())).thenReturn(List.of(renter));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of());

        var response = rentalHistoryService.getLentOverdueHistory(
                OWNER_ID,
                new PagingRequest(0, 20)
        );

        assertThat(response.content()).singleElement().satisfies(history -> {
            assertThat(history.counterparty().userId()).isEqualTo(RENTER_ID);
            assertThat(history.overdueDays()).isEqualTo(2);
        });
        verify(rentalHistoryRepository).findLentOverdueHistory(
                eq(OWNER_ID),
                any(LocalDate.class),
                eq(RentalOverduePolicy.statuses()),
                any(Pageable.class)
        );
    }

    @Test
    void 여러_빌린_이력도_장비_회원_썸네일을_각각_한_번만_일괄_조회한다() {
        Equipment firstEquipment = equipment(10L, 2L, "첫 장비");
        Equipment secondEquipment = equipment(20L, 3L, "둘째 장비");
        Rental firstRental = rental(
                201L,
                10L,
                RENTER_ID,
                RentalStatus.COMPLETED,
                LocalDate.now(),
                "첫 장비 스냅샷"
        );
        Rental secondRental = rental(
                202L,
                20L,
                RENTER_ID,
                RentalStatus.COMPLETED,
                LocalDate.now(),
                "둘째 장비 스냅샷"
        );

        when(rentalHistoryRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(firstRental, secondRental), PageRequest.of(0, 20), 2));
        when(equipmentRepository.findAllById(any())).thenReturn(List.of(firstEquipment, secondEquipment));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(2L, "첫 등록자"), user(3L, "둘째 등록자")));
        when(equipmentImageRepository.findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of(
                        thumbnail(1L, firstEquipment, "first-old.jpg", 1),
                        thumbnail(2L, firstEquipment, "first-new.jpg", 2),
                        thumbnail(3L, secondEquipment, "second.jpg", 1)
                ));

        var response = rentalHistoryService.getBorrowedHistory(
                RENTER_ID,
                new RentalHistorySearchRequest(null, null, 0, 20)
        );

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).thumbnailUrl()).isEqualTo("first-old.jpg");
        assertThat(response.content().get(1).thumbnailUrl()).isEqualTo("second.jpg");
        verify(equipmentRepository, times(1)).findAllById(any());
        verify(userRepository, times(1)).findAllById(any());
        verify(equipmentImageRepository, times(1))
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection());
    }

    private Page<Rental> page(Rental rental) {
        return new PageImpl<>(List.of(rental), PageRequest.of(0, 20), 1);
    }

    private Rental rental(
            Long id,
            Long equipmentId,
            Long renterId,
            RentalStatus status,
            LocalDate endDate,
            String productNameSnapshot
    ) {
        return Rental.builder()
                .id(id)
                .equipmentId(equipmentId)
                .renterId(renterId)
                .startDate(endDate.minusDays(2))
                .endDate(endDate)
                .productNameSnapshot(productNameSnapshot)
                .categorySnapshot("디지털기기")
                .dailyPriceSnapshot(BigDecimal.valueOf(10_000))
                .rentalDays(3)
                .totalPrice(BigDecimal.valueOf(30_000))
                .status(status)
                .build();
    }

    private Equipment equipment(Long id, Long ownerId, String name) {
        return Equipment.builder()
                .id(id)
                .ownerId(ownerId)
                .category(EquipmentCategory.OTHER)
                .name(name)
                .dailyPrice(BigDecimal.valueOf(99_999))
                .status(EquipmentStatus.ACTIVE)
                .productCondition(ProductConditionType.NORMAL)
                .build();
    }

    private User user(Long id, String nickname) {
        return User.builder()
                .id(id)
                .email("user" + id + "@iter.test")
                .password("encoded-password")
                .name("사용자" + id)
                .nickname(nickname)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private EquipmentImage thumbnail(
            Long id,
            Equipment equipment,
            String imageUrl,
            int sortOrder
    ) {
        return EquipmentImage.builder()
                .id(id)
                .equipment(equipment)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .thumbnail(true)
                .build();
    }

    private List<String> sortDescription(Pageable pageable) {
        return pageable.getSort().stream()
                .map(order -> order.getProperty() + ": " + order.getDirection())
                .toList();
    }
}
