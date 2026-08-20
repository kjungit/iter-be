package com.example.iter.notification.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// receiverId/rentalId는 각각 auth/reservation 도메인 PK를 값으로만 참조 (도메인 간 결합 최소화 컨벤션).
@Entity
@Table(name = "notification")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public boolean isReceiver(Long userId) {
        return this.receiverId.equals(userId);
    }

    public void markRead() {
        if (this.read) {
            return;
        }
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
