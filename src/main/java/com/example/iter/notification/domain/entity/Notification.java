package com.example.iter.notification.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

// receiverId/rentalId는 각각 auth/reservation 도메인 PK를 값으로만 참조 (도메인 간 결합 최소화 컨벤션).
// title/message(완성된 문장)를 저장하지 않는다 — params(보간 값)만 저장하고, 화면 표시 문구는
// 프론트가 type + params로 자체 i18n 사전을 통해 조립한다 (docs/i18n-frontend-handoff.md 참고).
// 이메일 발송용 문구는 NotificationService.create() 호출 시점에 NotificationMessages로 만들어서
// 그 자리에서만 쓰고 저장하지 않는다.
@Entity
@Table(
        name = "notification",
        indexes = {
                @Index(
                        name = "idx_notification_receiver_created_id",
                        columnList = "receiver_id, created_at DESC, id DESC"
                ),
                @Index(
                        name = "idx_notification_receiver_read_created_id",
                        columnList = "receiver_id, is_read, created_at DESC, id DESC"
                )
        }
)
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

    @Convert(converter = NotificationParamsConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, Object> params = Map.of();

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
