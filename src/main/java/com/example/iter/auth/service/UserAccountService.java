package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.PasswordChangeRequest;
import com.example.iter.auth.dto.request.UserDeleteRequest;
import com.example.iter.auth.dto.request.UserUpdateRequest;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.policy.RentalStatusPolicy;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, UserUpdateRequest request) {
        User user = findUserWithLock(userId);
        user.updateProfile(
                request.name() == null ? user.getName() : request.name(),
                request.nickname() == null ? user.getNickname() : request.nickname(),
                request.phone() == null ? user.getPhone() : request.phone()
        );
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findUserWithLock(userId);
        String encodedCurrentPassword = user.getPassword();

        if (encodedCurrentPassword == null) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_SET);
        }
        if (!passwordEncoder.matches(request.currentPassword(), encodedCurrentPassword)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        if (passwordEncoder.matches(request.newPassword(), encodedCurrentPassword)) {
            throw new CustomException(ErrorCode.SAME_PASSWORD_NOT_ALLOWED);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllByUserId(userId);
    }

    @Transactional
    public void withdraw(Long userId, UserDeleteRequest request) {
        User user = findUserWithLock(userId);
        validateWithdrawalPassword(user, request == null ? null : request.password());

        if (hasWithdrawalBlockingRental(userId)) {
            throw new CustomException(ErrorCode.ACTIVE_RENTAL_EXISTS);
        }

        LocalDateTime withdrawnAt = LocalDateTime.now();
        equipmentRepository.updateStatusByOwnerId(userId, EquipmentStatus.DELETED, withdrawnAt);
        user.withdraw(withdrawnAt);
        refreshTokenService.revokeAllByUserId(userId);
    }

    private void validateWithdrawalPassword(User user, String rawPassword) {
        if (user.getPassword() == null) {
            return;
        }
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }

    private boolean hasWithdrawalBlockingRental(Long userId) {
        var blockingStatuses = RentalStatusPolicy.withdrawalBlockingStatuses();
        return rentalRepository.countByRenterIdAndStatusIn(userId, blockingStatuses) > 0
                || rentalRepository.countLentByOwnerIdAndStatusIn(userId, blockingStatuses) > 0;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUserWithLock(Long userId) {
        return userRepository.findWithLockById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
