package com.example.iter.auth.domain.entity;

// ERD USER.status
public enum UserStatus {
    ACTIVE,     // 정상적으로 활동 가능한 회원
    SUSPENDED,  // 관리자에 의해 정지된 회원
    DELETED     // 탈퇴한 회원
}
