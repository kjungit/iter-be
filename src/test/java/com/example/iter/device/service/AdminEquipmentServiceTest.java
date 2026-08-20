package com.example.iter.device.service;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.service.AdminActionService;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.dto.request.AdminEquipmentSearchRequest;
import com.example.iter.device.dto.request.AdminEquipmentStatusRequest;
import com.example.iter.device.util.AdminEquipmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEquipmentServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long OWNER_ID = 2L;
    private static final Long EQUIPMENT_ID = 10L;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentImageRepository equipmentImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionService adminActionService;

    @Spy
    private AdminEquipmentMapper adminEquipmentMapper = new AdminEquipmentMapper();

    @InjectMocks
    private AdminEquipmentService adminEquipmentService;

    @Test
    void 관리자_장비_목록은_검색어를_정규화하고_등록자와_썸네일을_일괄_조회한다() {
        Equipment first = equipment(EQUIPMENT_ID, OWNER_ID, EquipmentStatus.ACTIVE, "맥북 프로");
        Equipment second = equipment(11L, OWNER_ID, EquipmentStatus.ACTIVE, "맥북 에어");
        User owner = owner();
        EquipmentImage firstThumbnail = image(100L, first, "https://example.com/first.jpg", 1, true);
        EquipmentImage duplicateThumbnail = image(101L, first, "https://example.com/second.jpg", 2, true);
        EquipmentImage secondThumbnail = image(102L, second, "https://example.com/air.jpg", 1, true);

        AdminEquipmentSearchRequest request = new AdminEquipmentSearchRequest(
                "  맥북  ",
                "  LAPTOP  ",
                EquipmentStatus.ACTIVE,
                1,
                10
        );
        when(equipmentRepository.searchForAdmin(
                eq("맥북"),
                eq("LAPTOP"),
                eq(EquipmentStatus.ACTIVE),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 10), 12));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(owner));
        when(equipmentImageRepository
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of(firstThumbnail, duplicateThumbnail, secondThumbnail));

        var response = adminEquipmentService.getEquipments(request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().owner().userId()).isEqualTo(OWNER_ID);
        assertThat(response.content().getFirst().thumbnailUrl())
                .isEqualTo("https://example.com/first.jpg");
        assertThat(response.content().get(1).thumbnailUrl())
                .isEqualTo("https://example.com/air.jpg");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(12);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(equipmentRepository).searchForAdmin(
                eq("맥북"),
                eq("LAPTOP"),
                eq(EquipmentStatus.ACTIVE),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
        verify(userRepository).findAllById(anyCollection());
        verify(equipmentImageRepository)
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection());
    }

    @Test
    void 빈_검색어와_카테고리는_null로_변환하고_빈_목록이면_추가_조회하지_않는다() {
        when(equipmentRepository.searchForAdmin(
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = adminEquipmentService.getEquipments(
                new AdminEquipmentSearchRequest("  ", " ", null, null, null)
        );

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        verify(userRepository, never()).findAllById(anyCollection());
        verify(equipmentImageRepository, never())
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection());
    }

    @Test
    void 특수문자_검색어를_변경하지_않고_저장소에_전달한다() {
        when(equipmentRepository.searchForAdmin(
                eq("%_"),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        adminEquipmentService.getEquipments(
                new AdminEquipmentSearchRequest("  %_  ", null, null, 0, 20)
        );

        verify(equipmentRepository).searchForAdmin(
                eq("%_"),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void 장비_목록의_등록자_정보가_없으면_조회할_수_없다() {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, EquipmentStatus.ACTIVE, "맥북 프로");
        when(equipmentRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(equipment)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(equipmentImageRepository
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> adminEquipmentService.getEquipments(
                new AdminEquipmentSearchRequest(null, null, null, 0, 20)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 관리자가_장비_상세와_정렬된_이미지를_조회한다() {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, EquipmentStatus.ACTIVE, "맥북 프로");
        User owner = owner();
        EquipmentImage image = image(100L, equipment, "https://example.com/image.jpg", 1, true);
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(equipmentImageRepository.findByEquipmentIdOrderBySortOrderAsc(EQUIPMENT_ID))
                .thenReturn(List.of(image));

        var response = adminEquipmentService.getEquipmentDetail(EQUIPMENT_ID);

        assertThat(response.equipmentId()).isEqualTo(EQUIPMENT_ID);
        assertThat(response.owner().userId()).isEqualTo(OWNER_ID);
        assertThat(response.name()).isEqualTo("맥북 프로");
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().imageUrl())
                .isEqualTo("https://example.com/image.jpg");
    }

    @Test
    void 존재하지_않는_장비_상세는_조회할_수_없다() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEquipmentService.getEquipmentDetail(EQUIPMENT_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_FOUND);

        verifyNoInteractions(userRepository, equipmentImageRepository, adminEquipmentMapper);
    }

    @ParameterizedTest
    @MethodSource("suspendableStatuses")
    void ACTIVE와_INACTIVE_장비를_차단하고_관리자_조치_이력을_기록한다(
            EquipmentStatus currentStatus
    ) {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, currentStatus, "맥북 프로");
        prepareStatusUpdate(equipment);

        var response = adminEquipmentService.updateEquipmentStatus(
                ADMIN_ID,
                EQUIPMENT_ID,
                new AdminEquipmentStatusRequest(
                        EquipmentStatus.SUSPENDED,
                        "  신고 누적  "
                )
        );

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.SUSPENDED);
        assertThat(response.status()).isEqualTo(EquipmentStatus.SUSPENDED);
        verify(adminActionService).record(
                ADMIN_ID,
                AdminActionTargetType.EQUIPMENT,
                EQUIPMENT_ID,
                AdminActionType.SUSPEND_EQUIPMENT,
                "신고 누적"
        );
        verify(equipmentRepository).flush();
    }

    @Test
    void SUSPENDED_장비를_INACTIVE로_복구하고_관리자_조치_이력을_기록한다() {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, EquipmentStatus.SUSPENDED, "맥북 프로");
        prepareStatusUpdate(equipment);

        var response = adminEquipmentService.updateEquipmentStatus(
                ADMIN_ID,
                EQUIPMENT_ID,
                new AdminEquipmentStatusRequest(
                        EquipmentStatus.INACTIVE,
                        "차단 사유 해소"
                )
        );

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.INACTIVE);
        assertThat(response.status()).isEqualTo(EquipmentStatus.INACTIVE);
        verify(adminActionService).record(
                ADMIN_ID,
                AdminActionTargetType.EQUIPMENT,
                EQUIPMENT_ID,
                AdminActionType.RESTORE_EQUIPMENT,
                "차단 사유 해소"
        );
        verify(equipmentRepository).flush();
    }

    @ParameterizedTest
    @MethodSource("invalidStatusTransitions")
    void 허용되지_않는_장비_상태_전이는_거절한다(
            EquipmentStatus currentStatus,
            EquipmentStatus requestedStatus
    ) {
        Equipment equipment = equipment(EQUIPMENT_ID, OWNER_ID, currentStatus, "맥북 프로");
        when(equipmentRepository.findByIdForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment));

        assertThatThrownBy(() -> adminEquipmentService.updateEquipmentStatus(
                ADMIN_ID,
                EQUIPMENT_ID,
                new AdminEquipmentStatusRequest(requestedStatus, "상태 변경")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EQUIPMENT_STATUS_TRANSITION);

        assertThat(equipment.getStatus()).isEqualTo(currentStatus);
        verifyNoInteractions(adminActionService);
        verify(equipmentRepository, never()).flush();
        verifyNoInteractions(userRepository, equipmentImageRepository, adminEquipmentMapper);
    }

    @Test
    void 존재하지_않는_장비의_상태는_변경할_수_없다() {
        when(equipmentRepository.findByIdForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEquipmentService.updateEquipmentStatus(
                ADMIN_ID,
                EQUIPMENT_ID,
                new AdminEquipmentStatusRequest(
                        EquipmentStatus.SUSPENDED,
                        "차단 시도"
                )
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_FOUND);

        verifyNoInteractions(adminActionService, userRepository, equipmentImageRepository, adminEquipmentMapper);
    }

    private void prepareStatusUpdate(Equipment equipment) {
        when(equipmentRepository.findByIdForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        when(equipmentImageRepository.findByEquipmentIdOrderBySortOrderAsc(EQUIPMENT_ID))
                .thenReturn(List.of());
    }

    private Equipment equipment(
            Long id,
            Long ownerId,
            EquipmentStatus status,
            String name
    ) {
        return Equipment.builder()
                .id(id)
                .ownerId(ownerId)
                .category(EquipmentCategory.LAPTOP)
                .name(name)
                .description("테스트 장비")
                .dailyPrice(BigDecimal.valueOf(30000))
                .availableFrom(LocalDate.of(2026, 8, 1))
                .availableTo(LocalDate.of(2026, 8, 31))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .conditionDetail("정상")
                .build();
    }

    private User owner() {
        return User.builder()
                .id(OWNER_ID)
                .email("owner@iter.test")
                .password("encoded-password")
                .name("장비 등록자")
                .nickname("등록자")
                .phone("010-0000-0002")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private EquipmentImage image(
            Long id,
            Equipment equipment,
            String url,
            int sortOrder,
            boolean thumbnail
    ) {
        return EquipmentImage.builder()
                .id(id)
                .equipment(equipment)
                .imageUrl(url)
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .build();
    }

    private static Stream<EquipmentStatus> suspendableStatuses() {
        return Stream.of(
                EquipmentStatus.ACTIVE,
                EquipmentStatus.INACTIVE
        );
    }

    private static Stream<Arguments> invalidStatusTransitions() {
        return Stream.of(
                Arguments.of(EquipmentStatus.ACTIVE, EquipmentStatus.ACTIVE),
                Arguments.of(EquipmentStatus.INACTIVE, EquipmentStatus.ACTIVE),
                Arguments.of(EquipmentStatus.SUSPENDED, EquipmentStatus.SUSPENDED),
                Arguments.of(EquipmentStatus.MAINTENANCE, EquipmentStatus.SUSPENDED),
                Arguments.of(EquipmentStatus.DELETED, EquipmentStatus.ACTIVE)
        );
    }
}
