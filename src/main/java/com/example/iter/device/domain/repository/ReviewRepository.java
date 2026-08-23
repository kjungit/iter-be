package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 리뷰 기능은 현재 비활성화되어 애플리케이션 조회에서 사용하지 않습니다.
 * 기존 스키마와 향후 복구 가능성을 위해 기본 Repository 선언만 유지합니다.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {
}
