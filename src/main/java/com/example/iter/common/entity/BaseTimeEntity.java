package com.example.iter.common.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// created_at / updated_at 두 컬럼을 모두 갖는 엔티티가 상속하는 공통 클래스.
// (개발 컨벤션 문서에 별도로 명시되어 있지는 않지만, ERD의 모든 엔티티가
//  created_at/updated_at 컬럼을 갖고 있어 중복 방지 차원에서 추가함 — 팀 컨벤션에 맞게 위치 조정 가능)
// 사용하려면 JpaConfig에 @EnableJpaAuditing 이 켜져 있어야 한다.
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
