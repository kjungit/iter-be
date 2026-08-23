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

    // offset(LIMIT/OFFSET) 대신 keyset(cursor) 방식: cursorId보다 작은(=더 이전) 알림을 id 역순으로 가져온다.
    // 알림은 insert만 되는 append-only 데이터라 id 순서가 곧 시간 순서와 같아, id 하나만으로 커서를 안전하게 쓸 수 있다.
    // pageable은 offset 없이 LIMIT(=size + 1)로만 쓰인다 (다음 페이지 존재 여부를 count 쿼리 없이 판단하기 위함).
    @Query("""
            select n
            from Notification n
            where n.receiverId = :receiverId
              and (:cursorId is null or n.id < :cursorId)
            order by n.id desc
            """)
    List<Notification> findNextByReceiverId(
            @Param("receiverId") Long receiverId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select n
            from Notification n
            where n.receiverId = :receiverId
              and n.read = false
              and (:cursorId is null or n.id < :cursorId)
            order by n.id desc
            """)
    List<Notification> findNextUnreadByReceiverId(
            @Param("receiverId") Long receiverId,
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
