package com.example.iter.auth.util;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserMapperTest {

    private final AdminUserMapper adminUserMapper = new AdminUserMapper();

    @Test
    void 회원과_집계값을_관리자_회원_상세_응답으로_변환한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 2, 11, 30);
        User user = User.builder()
                .id(2L)
                .email("user@iter.test")
                .password("encoded-password")
                .name("테스트 회원")
                .nickname("iter회원")
                .phone("010-1234-5678")
                .role(Role.USER)
                .status(UserStatus.SUSPENDED)
                .build();
        ReflectionTestUtils.setField(user, "createdAt", createdAt);
        ReflectionTestUtils.setField(user, "updatedAt", updatedAt);

        AdminUserDetailResponse response = adminUserMapper.toDetail(
                user,
                3L,
                4L,
                1L,
                2L
        );

        assertThat(response).isEqualTo(new AdminUserDetailResponse(
                2L,
                "user@iter.test",
                "테스트 회원",
                "iter회원",
                "010-1234-5678",
                Role.USER,
                UserStatus.SUSPENDED,
                3L,
                4L,
                1L,
                2L,
                createdAt,
                updatedAt
        ));
    }
}
