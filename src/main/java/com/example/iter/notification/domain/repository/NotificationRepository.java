package com.example.iter.notification.domain.repository;

import com.example.iter.notification.domain.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // offset(LIMIT/OFFSET) 대신 keyset(cursor) 방식: (createdAt, id) 커서보다 이전 알림을 내림차순으로 가져온다.
    // createdAt만으로는 동시에 생성된 알림끼리 동점(tie)이 날 수 있어 id를 타이브레이커로 묶는다.
    // pageable은 offset 없이 LIMIT(=size + 1)로만 쓰인다 (다음 페이지 존재 여부를 count 쿼리 없이 판단하기 위함).
    @Query("""
            select n
            from Notification n
            where n.receiverId = :receiverId
              and (
                    :cursorCreatedAt is null
                    or n.createdAt < :cursorCreatedAt
                    or (n.createdAt = :cursorCreatedAt and n.id < :cursorId)
                  )
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findNextByReceiverId(
            @Param("receiverId") Long receiverId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select n
            from Notification n
            where n.receiverId = :receiverId
              and n.read = false
              and (
                    :cursorCreatedAt is null
                    or n.createdAt < :cursorCreatedAt
                    or (n.createdAt = :cursorCreatedAt and n.id < :cursorId)
                  )
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findNextUnreadByReceiverId(
            @Param("receiverId") Long receiverId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    long countByReceiverIdAndReadFalse(Long receiverId);

    // 목록 화면을 열람하는 시점에 한 번에 모두 읽음 처리 — 건수가 많을 수 있어 엔티티를 각각 불러오지 않고 벌크 업데이트로 처리.
    @Modifying
    @Query("""
            update Notification n
               set n.read = true, n.readAt = :readAt
             where n.receiverId = :receiverId
               and n.read = false
            """)
    int markAllAsRead(@Param("receiverId") Long receiverId, @Param("readAt") LocalDateTime readAt);
}
