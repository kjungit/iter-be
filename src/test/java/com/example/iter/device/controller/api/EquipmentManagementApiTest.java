package com.example.iter.device.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImageUpload;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentImageUploadRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.dto.request.EquipmentCreateRequest;
import com.example.iter.device.service.EquipmentManagementService;
import com.example.iter.device.storage.EquipmentImageStorage;
import com.example.iter.device.storage.PresignedUpload;
import com.example.iter.device.storage.StoredImage;
import com.example.iter.device.storage.ValidatedUpload;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentManagementApiTest {

    private static final long IMAGE_SIZE = 1024L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private EquipmentImageRepository equipmentImageRepository;
    @Autowired
    private EquipmentImageUploadRepository imageUploadRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private EquipmentManagementService equipmentManagementService;

    @MockitoBean
    private EquipmentImageStorage imageStorage;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        equipmentImageRepository.deleteAll();
        imageUploadRepository.deleteAll();
        equipmentRepository.deleteAll();
        userRepository.deleteAll();

        when(imageStorage.validateTemporaryUpload(anyString(), anyString(), anyLong()))
                .thenAnswer(invocation -> new ValidatedUpload(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        "etag-" + invocation.getArgument(0)));
        when(imageStorage.promote(anyLong(), any()))
                .thenAnswer(invocation -> {
                    Long equipmentId = invocation.getArgument(0);
                    ValidatedUpload upload = invocation.getArgument(1);
                    String finalKey = "equipment/%d/%s".formatted(
                            equipmentId,
                            upload.objectKey().substring(upload.objectKey().lastIndexOf('/') + 1));
                    return new StoredImage(
                            finalKey,
                            "https://cdn.example.com/" + finalKey
                    );
                });
    }

    @Test
    void 이미지_업로드용_Presigned_URL을_발급하고_소유권_레코드를_저장한다() throws Exception {
        User owner = saveUser("presign-owner@example.com", UserStatus.ACTIVE);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        when(imageStorage.createPresignedUpload(owner.getId(), "image/jpeg", IMAGE_SIZE))
                .thenReturn(new PresignedUpload(
                        "equipment/temp/%d/uuid.jpg".formatted(owner.getId()),
                        URI.create("https://s3.example.com/presigned").toURL(),
                        Map.of("content-type", "image/jpeg"),
                        expiresAt));

        mockMvc.perform(post("/api/v1/devices/images/presigned-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "files":[{
                                    "fileName":"camera.jpg",
                                    "contentType":"image/jpeg",
                                    "size":1024
                                  }]
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploads[0].objectKey")
                        .value("equipment/temp/%d/uuid.jpg".formatted(owner.getId())))
                .andExpect(jsonPath("$.uploads[0].uploadUrl")
                        .value("https://s3.example.com/presigned"))
                .andExpect(jsonPath("$.uploads[0].requiredHeaders.content-type")
                        .value("image/jpeg"));

        EquipmentImageUpload upload = imageUploadRepository.findAll().getFirst();
        assertThat(upload.getUserId()).isEqualTo(owner.getId());
        assertThat(upload.getExpectedSize()).isEqualTo(IMAGE_SIZE);
        assertThat(upload.isUsed()).isFalse();
    }

    @Test
    void Presigned_URL은_한번에_최대_5개까지만_발급한다() throws Exception {
        User owner = saveUser("presign-limit@example.com", UserStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/devices/images/presigned-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"files":[
                                  {"fileName":"1.jpg","contentType":"image/jpeg","size":1},
                                  {"fileName":"2.jpg","contentType":"image/jpeg","size":1},
                                  {"fileName":"3.jpg","contentType":"image/jpeg","size":1},
                                  {"fileName":"4.jpg","contentType":"image/jpeg","size":1},
                                  {"fileName":"5.jpg","contentType":"image/jpeg","size":1},
                                  {"fileName":"6.jpg","contentType":"image/jpeg","size":1}
                                ]}
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(imageUploadRepository.count()).isZero();
    }

    @Test
    void 검증된_임시_이미지를_승격하여_장비와_대표_이미지를_등록한다() throws Exception {
        User owner = saveUser("create-owner@example.com", UserStatus.ACTIVE);
        List<String> keys = List.of(
                savePendingUpload(owner, "front.jpg").getObjectKey(),
                savePendingUpload(owner, "back.jpg").getObjectKey());

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(keys, 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("CAMERA"))
                .andExpect(jsonPath("$.name").value("소니 A7C2"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].thumbnail").value(false))
                .andExpect(jsonPath("$.images[1].thumbnail").value(true));

        Equipment equipment = equipmentRepository.findAll().getFirst();
        var images = equipmentImageRepository.findByEquipmentIdOrderBySortOrderAsc(equipment.getId());
        assertThat(images).hasSize(2);
        assertThat(images).allMatch(image -> image.getObjectKey().startsWith(
                "equipment/%d/".formatted(equipment.getId())));
        assertThat(imageUploadRepository.findAll()).allMatch(EquipmentImageUpload::isUsed);
        keys.forEach(key -> verify(imageStorage).delete(key));
    }

    @Test
    void 모든_이미지_검증이_끝나기_전에는_승격을_시작하지_않는다() throws Exception {
        User owner = saveUser("invalid-image@example.com", UserStatus.ACTIVE);
        List<String> keys = List.of(
                savePendingUpload(owner, "valid.jpg").getObjectKey(),
                savePendingUpload(owner, "invalid.jpg").getObjectKey());
        when(imageStorage.validateTemporaryUpload(
                keys.get(1), "image/jpeg", IMAGE_SIZE))
                .thenThrow(new CustomException(ErrorCode.INVALID_IMAGE));

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(keys, 0))
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));

        assertThat(equipmentRepository.count()).isZero();
        assertThat(equipmentImageRepository.count()).isZero();
        org.mockito.Mockito.verify(imageStorage, org.mockito.Mockito.never())
                .promote(anyLong(), any());
    }

    @Test
    void 두번째_이미지_승격이_실패하면_DB와_먼저_승격한_객체를_정리한다() throws Exception {
        User owner = saveUser("rollback-owner@example.com", UserStatus.ACTIVE);
        List<String> keys = List.of(
                savePendingUpload(owner, "first.jpg").getObjectKey(),
                savePendingUpload(owner, "second.jpg").getObjectKey());
        StoredImage firstImage = new StoredImage(
                "equipment/1/first.jpg",
                "https://cdn.example.com/equipment/1/first.jpg");
        doReturn(firstImage)
                .doThrow(new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED))
                .when(imageStorage).promote(anyLong(), any());

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(keys, 0))
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_FAILED"));

        assertThat(equipmentRepository.count()).isZero();
        assertThat(equipmentImageRepository.count()).isZero();
        assertThat(imageUploadRepository.findAll()).noneMatch(EquipmentImageUpload::isUsed);
        verify(imageStorage).delete(firstImage.objectKey());
    }

    @Test
    void 다른_회원에게_발급된_이미지는_장비에_등록할_수_없다() throws Exception {
        User owner = saveUser("owner-image@example.com", UserStatus.ACTIVE);
        User attacker = saveUser("attacker@example.com", UserStatus.ACTIVE);
        String key = savePendingUpload(owner, "private.jpg").getObjectKey();

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(List.of(key), 0))
                        .header(HttpHeaders.AUTHORIZATION, bearer(attacker)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_NOT_FOUND"));
    }

    @Test
    void 이미_사용한_이미지_업로드는_다른_장비에_재사용할_수_없다() throws Exception {
        User owner = saveUser("used-upload@example.com", UserStatus.ACTIVE);
        EquipmentImageUpload upload = savePendingUpload(owner, "used.jpg");
        upload.use(LocalDateTime.now());
        imageUploadRepository.saveAndFlush(upload);

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(List.of(upload.getObjectKey()), 0))
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_ALREADY_USED"));
    }

    @Test
    void 만료된_이미지_업로드는_장비에_등록할_수_없다() throws Exception {
        User owner = saveUser("expired-upload@example.com", UserStatus.ACTIVE);
        EquipmentImageUpload upload = imageUploadRepository.saveAndFlush(
                EquipmentImageUpload.builder()
                        .userId(owner.getId())
                        .objectKey("equipment/temp/%d/expired.jpg".formatted(owner.getId()))
                        .expectedContentType("image/jpeg")
                        .expectedSize(IMAGE_SIZE)
                        .expiresAt(LocalDateTime.now().minusSeconds(1))
                        .build());

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(List.of(upload.getObjectKey()), 0))
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_EXPIRED"));
    }

    @Test
    void 동일한_임시_이미지로_동시에_등록하면_한_건만_성공한다() throws Exception {
        User owner = saveUser("concurrent-upload@example.com", UserStatus.ACTIVE);
        String objectKey = savePendingUpload(owner, "single-use.jpg").getObjectKey();
        EquipmentCreateRequest request = new EquipmentCreateRequest(
                EquipmentCategory.CAMERA,
                "동시 등록 카메라",
                "동일 업로드 재사용 테스트",
                BigDecimal.valueOf(30_000),
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                ProductConditionType.NORMAL,
                null,
                List.of(objectKey),
                0
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(
                    executor.submit(() -> createConcurrently(owner, request, ready, start)),
                    executor.submit(() -> createConcurrently(owner, request, ready, start))
            );
            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> outcomes = results.stream()
                    .map(future -> {
                        try {
                            return future.get(5, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            assertThat(outcomes.stream()
                    .filter(outcome -> outcome instanceof Long)
                    .count()).isEqualTo(1);
            assertThat(outcomes.stream()
                    .filter(outcome -> outcome == ErrorCode.IMAGE_UPLOAD_ALREADY_USED)
                    .count()).isEqualTo(1);
        }
        assertThat(equipmentRepository.count()).isEqualTo(1);
    }

    @Test
    void 전달한_필드만_수정하고_카테고리는_유지한다() throws Exception {
        User owner = saveUser("update-owner@example.com", UserStatus.ACTIVE);
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/devices/{equipmentId}", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"수정된 카메라",
                                  "dailyPrice":45000,
                                  "productCondition":"DAMAGED",
                                  "conditionDetail":"렌즈 캡에 흠집이 있습니다."
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 카메라"))
                .andExpect(jsonPath("$.dailyPrice").value(45000))
                .andExpect(jsonPath("$.category").value("CAMERA"))
                .andExpect(jsonPath("$.productCondition").value("DAMAGED"));
    }

    @Test
    void 기존_점유_예약을_제외하는_기간으로_수정할_수_없다() throws Exception {
        User owner = saveUser("period-owner@example.com", UserStatus.ACTIVE);
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);
        saveRental(equipment, RentalStatus.APPROVED,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

        mockMvc.perform(patch("/api/v1/devices/{equipmentId}", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availableTo":"%s"}
                                """.formatted(LocalDate.now().plusDays(12)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_RENTAL_EXISTS"));
    }

    @Test
    void 다른_회원은_장비를_수정하거나_삭제할_수_없다() throws Exception {
        User owner = saveUser("owner@example.com", UserStatus.ACTIVE);
        User other = saveUser("other@example.com", UserStatus.ACTIVE);
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/devices/{equipmentId}", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"탈취 시도\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/devices/{equipmentId}", equipment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 거래와_분쟁이_없으면_장비를_소프트_삭제한다() throws Exception {
        User owner = saveUser("delete-owner@example.com", UserStatus.ACTIVE);
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);

        mockMvc.perform(delete("/api/v1/devices/{equipmentId}", equipment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getStatus())
                .isEqualTo(EquipmentStatus.DELETED);
        mockMvc.perform(get("/api/v1/devices/{equipmentId}", equipment.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 진행_중_거래와_분쟁은_장비_삭제를_차단한다() throws Exception {
        User owner = saveUser("blocking-owner@example.com", UserStatus.ACTIVE);
        Equipment rentalEquipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);
        Equipment disputeEquipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);
        saveRental(rentalEquipment, RentalStatus.REQUESTED,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));
        saveRental(disputeEquipment, RentalStatus.DISPUTED,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));

        mockMvc.perform(delete("/api/v1/devices/{equipmentId}", rentalEquipment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_RENTAL_EXISTS"));

        mockMvc.perform(delete("/api/v1/devices/{equipmentId}", disputeEquipment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_DISPUTE_EXISTS"));
    }

    @Test
    void 소유자는_장비_공개를_중지하고_재개할_수_있다() throws Exception {
        User owner = saveUser("status-owner@example.com", UserStatus.ACTIVE);
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/devices/{equipmentId}/status", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/v1/devices/{equipmentId}/status", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void 소유자는_SUSPENDED나_DELETED를_요청할_수_없다() throws Exception {
        User owner = saveUser("invalid-status-owner@example.com", UserStatus.ACTIVE);
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/devices/{equipmentId}/status", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private User saveUser(String email, UserStatus status) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("encoded-password")
                .name("장비 소유자")
                .nickname("장비주인")
                .phone("010-1111-2222")
                .status(status)
                .build());
    }

    private EquipmentImageUpload savePendingUpload(User user, String filename) {
        return imageUploadRepository.saveAndFlush(EquipmentImageUpload.builder()
                .userId(user.getId())
                .objectKey("equipment/temp/%d/%s".formatted(user.getId(), filename))
                .expectedContentType("image/jpeg")
                .expectedSize(IMAGE_SIZE)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());
    }

    private Equipment saveEquipment(Long ownerId, EquipmentStatus status) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(ownerId)
                .category(EquipmentCategory.CAMERA)
                .name("기존 카메라")
                .description("기존 설명")
                .dailyPrice(BigDecimal.valueOf(30_000))
                .availableFrom(LocalDate.now())
                .availableTo(LocalDate.now().plusMonths(2))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private void saveRental(
            Equipment equipment,
            RentalStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        rentalRepository.saveAndFlush(Rental.builder()
                .equipmentId(equipment.getId())
                .renterId(999L)
                .startDate(startDate)
                .endDate(endDate)
                .productNameSnapshot(equipment.getName())
                .categorySnapshot(equipment.getCategory().name())
                .dailyPriceSnapshot(equipment.getDailyPrice())
                .rentalDays((int) (endDate.toEpochDay() - startDate.toEpochDay() + 1))
                .totalPrice(equipment.getDailyPrice())
                .status(status)
                .build());
    }

    private String createRequestJson(List<String> imageKeys, int thumbnailIndex) {
        String keys = imageKeys.stream()
                .map(key -> "\"" + key + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "category":"CAMERA",
                  "name":"소니 A7C2",
                  "description":"풀프레임 미러리스 카메라입니다.",
                  "dailyPrice":30000,
                  "availableFrom":"%s",
                  "availableTo":"%s",
                  "productCondition":"NORMAL",
                  "conditionDetail":null,
                  "imageKeys":[%s],
                  "thumbnailIndex":%d
                }
                """.formatted(
                LocalDate.now(), LocalDate.now().plusMonths(2), keys, thumbnailIndex);
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    private Object createConcurrently(
            User owner,
            EquipmentCreateRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return equipmentManagementService.create(owner, request).id();
        } catch (CustomException exception) {
            return exception.getErrorCode();
        }
    }
}
