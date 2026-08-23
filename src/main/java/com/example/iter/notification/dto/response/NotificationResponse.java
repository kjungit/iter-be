package com.example.iter.notification.dto.response;

import com.example.iter.notification.domain.entity.Notification;
import com.example.iter.notification.domain.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

// title/message(완성된 문장)가 아니라 type + params를 내려준다 — 프론트가 이 둘로
// 자체 i18n 사전에서 표시 문구(제목/본문)를 조립한다 (docs/i18n-frontend-handoff.md 참고).
public record NotificationResponse(
        Long id,
        NotificationType type,
        Map<String, Object> params,
        Long rentalId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getParams(),
                notification.getRentalId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
