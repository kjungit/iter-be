package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
                or lower(coalesce(u.nickName, ''))
                    like lower(concat('%', :keyword, '%'))
              )
        """)
    Page<User> searchForAdmin(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    // 회원 상태를 안전하게 변경할 수 있도록 대상 회원 행을 비관적 쓰기 락으로 조회합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockById(Long userid);
}
