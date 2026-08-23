package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.AdminUserSearchRequest;
import com.example.iter.auth.dto.request.AdminUserStatusRequest;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import com.example.iter.auth.dto.response.AdminUserStatusResponse;
import com.example.iter.auth.dto.response.AdminUserSummaryResponse;
import com.example.iter.auth.util.AdminUserMapper;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.service.AdminActionService;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalRepository rentalRepository;
    private final ReportRepository reportRepository;
    private final AdminActionService adminActionService;
    private final AdminUserMapper adminUserMapper;


     // 실제 거래가 성립된 것으로 집계할 대여 상태 목록입니다.
     // 대여 횟수와 대여 제공 횟수를 계산할 때 사용합니다.
    private static final Set<RentalStatus> ESTABLISHED_STATUSES = EnumSet.of(
            RentalStatus.APPROVED,          // 장비 등록자가 대여 요청을 승인한 상태
            RentalStatus.SHIPPING,          // 장비를 대여자에게 배송 중인 상태
            RentalStatus.RECEIVED,          // 대여자가 장비 수령을 확인한 상태
            RentalStatus.RENTING,           // 장비를 실제로 대여 중인 상태
            RentalStatus.RETURN_REQUESTED,  // 대여자가 반납을 신청한 상태
            RentalStatus.RETURNING,         // 장비를 등록자에게 반송 중인 상태
            RentalStatus.RETURNED,          // 반송이 완료되어 반납 확인 단계인 상태
            RentalStatus.DISPUTED,          // 반납 과정에서 문제가 발생해 분쟁 중인 상태
            RentalStatus.COMPLETED          // 반납 확인까지 끝나 거래가 최종 완료된 상태
    );


    // 대여 종료일이 지났을 때 연체 거래로 집계할 수 있는 대여 상태 목록입니다.
    // 장비가 아직 등록자에게 정상적으로 반환되지 않은 상태만 포함합니다.
    private static final Set<RentalStatus> OVERDUE_STATUSES = EnumSet.of(
            RentalStatus.RECEIVED,          // 대여자가 장비를 수령했고 아직 반환하지 않은 상태
            RentalStatus.RENTING,           // 장비를 실제로 대여 중인 상태
            RentalStatus.RETURN_REQUESTED,  // 반납을 신청했지만 아직 반송하지 않은 상태
            RentalStatus.RETURNING          // 반송 중이지만 등록자에게 아직 도착하지 않은 상태
    );

    // 관리자 회원 목록을 검색 조건과 커서 정보로 조회합니다.
    @Transactional(readOnly = true)
    public CursorPageResponse<AdminUserSummaryResponse> getUsers(AdminUserSearchRequest request) {
        String keyword = StringUtils.hasText(request.keyword())
                ? request.keyword().trim()
                : null;
        CursorKey cursorKey = CursorCodec.decode(request.cursor());

        List<User> users = userRepository.searchForAdminByCursor(
                keyword,
                request.status(),
                cursorKey == null ? null : cursorKey.createdAt(),
                cursorKey == null ? null : cursorKey.id(),
                PageRequest.of(0, request.size() + 1)
        );

        return CursorPageResponse.from(
                users,
                request.size(),
                AdminUserSummaryResponse::from,
                user -> new CursorKey(user.getCreatedAt(), user.getId())
        );
    }

    // 회원 정보와 거래, 연체, 신고 집계값을 조회합니다
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long rentedCount = rentalRepository.countByRenterIdAndStatusIn(userId, ESTABLISHED_STATUSES);
        long lentCount = rentalRepository.countLentByOwnerIdAndStatusIn(userId, ESTABLISHED_STATUSES);
        long overdueCount = rentalRepository.countByRenterIdAndEndDateBeforeAndStatusIn(userId, LocalDate.now(), OVERDUE_STATUSES);
        long reportCount = reportRepository.countByTargetTypeAndTargetId(ReportTargetType.USER, userId);

        return adminUserMapper.toDetail(
                user,
                rentedCount,
                lentCount,
                overdueCount,
                reportCount
        );
    }

    // 회원 상태를 ACTIVE 또는 SUSPENDED로 변경하고 관리자 조치 이력을 저장합니다.
    @Transactional
    public AdminUserStatusResponse updateStatus(Long adminId, Long userId, AdminUserStatusRequest request) {
        User user = userRepository.findWithLockById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateStatusChange(user, request.status());
        AdminActionType action = applyStatus(user, request.status());

        adminActionService.record(
                adminId,
                AdminActionTargetType.USER,
                user.getId(),
                action,
                request.reason().trim()
        );

        userRepository.flush();

        log.info(
                "관리자 회원 상태 변경 처리: adminId={}, userId={}, action={}, status={}",
                adminId,
                userId,
                action,
                user.getStatus()
        );

        return AdminUserStatusResponse.from(user);
    }

    // 회원 상태 변경 가능 여부를 검증합니다.
    private void validateStatusChange(User user, UserStatus requestedStatus) {
        if (user.getRole() == Role.ADMIN && requestedStatus == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ADMIN_SUSPENSION_NOT_ALLOWED);
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS_TRANSITION);
        }

        if (user.getStatus() == requestedStatus) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS_TRANSITION);
        }
    }


    // 회원 상태를 변경하고 기록할 관리자 조치 유형을 반환합니다.
    private AdminActionType applyStatus(User user, UserStatus requestedStatus) {
        if (requestedStatus == UserStatus.SUSPENDED) {
            user.suspend();
            return AdminActionType.SUSPEND_USER;
        }

        if (requestedStatus == UserStatus.ACTIVE) {
            user.restore();
            return AdminActionType.RESTORE_USER;
        }

        throw new CustomException(
                ErrorCode.INVALID_USER_STATUS_TRANSITION
        );
    }




}
