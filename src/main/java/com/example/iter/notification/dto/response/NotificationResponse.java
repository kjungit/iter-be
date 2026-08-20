package com.example.iter.notification.dto.response;

import com.example.iter.notification.domain.entity.Notification;
import com.example.iter.notification.domain.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long rentalId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRentalId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
