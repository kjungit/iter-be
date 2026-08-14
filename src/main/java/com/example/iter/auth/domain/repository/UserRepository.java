package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT new com.example.iter.auth.dto.response.UserSummaryResponse(u.id, u.nickname) " +
            "FROM User u WHERE u.id = :id")
    Optional<UserSummaryResponse> findSummaryById(@Param("id") Long id);

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
