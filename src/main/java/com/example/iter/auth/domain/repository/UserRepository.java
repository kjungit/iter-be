package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.auth.domain.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select new com.example.iter.auth.dto.response.UserSummaryResponse(u.id, u.nickname)
            from User u
            where u.id = :id
            """)
    Optional<UserSummaryResponse> findSummaryById(@Param("id") Long id);

    // 받은 대여 요청 목록에 필요한 회원 ID와 닉네임만 한 번에 조회합니다.
    @Query("SELECT new com.example.iter.auth.dto.response.UserSummaryResponse(u.id, u.nickname) " +
            "FROM User u WHERE u.id IN :ids")
    List<UserSummaryResponse> findSummariesByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * 상태가 null이면 전체 상태를 조회하고, 검색어가 null이면 검색 조건을 적용하지 않습니다.
     * 검색어가 있으면 이메일·이름·닉네임에 검색어가 포함된 회원을 대소문자 구분 없이 조회합니다.
     * LOCATE를 사용해 %, _를 와일드카드가 아닌 실제 검색 문자로 처리합니다.
     * 상태와 검색어가 모두 있으면 두 조건을 모두 만족하는 회원을 커서 다음 위치부터 반환합니다.
     */
    @Query("""
        select u
        from User u
        where (:status is null or u.status = :status)
          and (
                :keyword is null
                or locate(lower(:keyword), lower(u.email)) > 0
                or locate(lower(:keyword), lower(u.name)) > 0
                or locate(lower(:keyword), lower(coalesce(u.nickname, ''))) > 0
              )
          and (
                :cursorCreatedAt is null
                or u.createdAt < :cursorCreatedAt
                or (u.createdAt = :cursorCreatedAt and u.id < :cursorId)
              )
        order by u.createdAt desc, u.id desc
        """)
    List<User> searchForAdminByCursor(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 회원 상태를 안전하게 변경할 수 있도록 대상 회원 행을 비관적 쓰기 락으로 조회합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockById(Long userId);
}
