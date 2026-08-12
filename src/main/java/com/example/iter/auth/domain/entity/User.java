package com.example.iter.auth.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

// ERD USER 엔티티
// id, email(UK), password, name, nickname, phone, role, status, created_at, updated_at
@Entity
@Table(name = "users") // "user"는 MySQL 예약어와 충돌 위험이 있어 users로 지정
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "nick_name", length = 20)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    // ===== 도메인 메서드 =====

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfile(String name, String nickname, String phone) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void restore() {
        this.status = UserStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = UserStatus.DELETED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
}
