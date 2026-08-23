package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.PreferredLanguage;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.request.SignUpRequest;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponse signUp(SignUpRequest request, PreferredLanguage preferredLanguage) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickname(request.nickname())
                .phone(request.phone())
                .preferredLanguage(preferredLanguage)
                .build();

        User savedUser = userRepository.save(user);
        log.info("회원가입 처리: userId={}", savedUser.getId());
        return UserResponse.from(savedUser);
    }

    @Transactional
    public IssuedTokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        IssuedTokenPair tokenPair = issueTokens(user);
        log.info("로그인 처리: userId={}, role={}", user.getId(), user.getRole());
        return tokenPair;
    }

    public IssuedTokenPair issueTokens(User user) {
        validateLoginAllowed(user);
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = refreshTokenService.issueForLogin(user);
        return new IssuedTokenPair(accessToken, refreshToken);
    }

    public IssuedTokenPair refresh(String rawRefreshToken) {
        return refreshTokenService.rotate(rawRefreshToken);
    }

    public void logout(Long userId, String rawRefreshToken) {
        refreshTokenService.revoke(userId, rawRefreshToken);
        log.info("로그아웃 처리: userId={}", userId);
    }

    private void validateLoginAllowed(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
    }
}
