package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * 상태가 null이면 전체 상태를 조회하고, 검색어가 null이면 검색 조건을 적용하지 않습니다.
     * 검색어가 있으면 이메일·이름·닉네임에 검색어가 포함된 회원을 대소문자 구분 없이 조회합니다.
     * 상태와 검색어가 모두 있으면 두 조건을 모두 만족하는 회원을 페이지 단위로 반환합니다.
     */
    @Query("""
        select u
        from User u
        where (:status is null or u.status = :status)
          and (
                :keyword is null
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(u.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.nickname, ''))
                    like lower(concat('%', :keyword, '%'))
              )
        """)
    Page<User> searchForAdmin(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    // 회원 상태를 안전하게 변경할 수 있도록 대상 회원 행을 비관적 쓰기 락으로 조회합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockById(Long userId);
    // 잔액 확인과 차감을 하나의 조건부 UPDATE로 원자적으로 처리해서 여러 결제가 동시에 들어와도 잔액보다 많은 금액이 차감되는 것을 방지한다
    // — 조건부 UPDATE. 영향 row 0건이면 포인트 부족을 의미.
    @Modifying
    @Query("UPDATE User u SET u.pointBalance = u.pointBalance - :amount " +
            "WHERE u.id = :userId AND u.pointBalance >= :amount")
    int deductPointBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE User u SET u.pointBalance = u.pointBalance + :amount WHERE u.id = :userId")
    int refundPointBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
