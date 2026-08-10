package com.example.iter.common.security;

import com.example.iter.auth.domain.entity.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Spring Security와 도메인 User 엔티티 사이의 어댑터.
// token 프로젝트와 동일한 이유로, User 엔티티가 UserDetails를 직접 구현하지 않고 감싸는 방식을 사용한다.
// @AuthenticationPrincipal CustomUserDetails 로 컨트롤러/서비스에서 바로 꺼내 쓸 수 있다.
@Getter
@Builder
public class CustomUserDetails implements UserDetails {

    private final User user;

    // ROLE_ 접두사는 Spring Security 표준 규칙 — hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN" 권한을 찾는다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 시큐리티의 username = 로그인 식별자. 이 프로젝트는 email로 로그인하므로 email을 반환한다.
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 정지(SUSPENDED)된 계정은 잠금 처리로 간주 -> 로그인 자체를 막는다.
        return user.getStatus() != com.iter.auth.domain.entity.UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 탈퇴(DELETED)한 계정은 비활성 처리
        return user.getStatus() != com.iter.auth.domain.entity.UserStatus.DELETED;
    }
}
