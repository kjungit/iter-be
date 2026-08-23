package com.example.iter.notification.domain.repository;

import com.example.iter.notification.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByCreatedAtDescIdDesc(Long receiverId, Pageable pageable);

    Page<Notification> findByReceiverIdAndReadFalseOrderByCreatedAtDescIdDesc(Long receiverId, Pageable pageable);

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
