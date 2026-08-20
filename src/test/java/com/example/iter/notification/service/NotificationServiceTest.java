package com.example.iter.notification.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.mail.MailMessage;
import com.example.iter.common.mail.MailService;
import com.example.iter.notification.config.NotificationProperties;
import com.example.iter.notification.domain.entity.Notification;
import com.example.iter.notification.domain.entity.NotificationType;
import com.example.iter.notification.domain.repository.NotificationRepository;
import com.example.iter.notification.dto.response.NotificationResponse;
import com.example.iter.notification.sse.NotificationSseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationSseService notificationSseService;
    @Mock
    private MailService mailService;

    private final NotificationProperties notificationProperties =
            new NotificationProperties("noreply@iter.example.com", "notification:user:");

    private NotificationService notificationService() {
        return new NotificationService(notificationRepository, notificationSseService, mailService, notificationProperties);
    }

    private Notification notification(Long id, Long receiverId, boolean read) {
        return Notification.builder()
                .id(id)
                .receiverId(receiverId)
                .type(NotificationType.RENTAL_APPROVED)
                .title("제목")
                .message("내용")
                .rentalId(10L)
                .read(read)
                .build();
    }

    @Test
    void 메일이_필요한_타입이면_SSE와_메일을_모두_보낸다() {
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService().create(1L, "owner@test.com", NotificationType.RENTAL_REQUESTED,
                "새로운 대여 신청", "메시지", 10L);

        verify(notificationSseService).send(eq(1L), any(NotificationResponse.class));
        verify(mailService).send(new MailMessage("owner@test.com", "noreply@iter.example.com", "새로운 대여 신청", "메시지"));
    }

    @Test
    void 메일이_필요없는_타입이면_SSE만_보낸다() {
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService().create(2L, "renter@test.com", NotificationType.PAYMENT_COMPLETED_RENTER,
                "결제 완료", "메시지", 10L);

        verify(notificationSseService).send(eq(2L), any(NotificationResponse.class));
        verify(mailService, never()).send(any());
    }

    @Test
    void 본인_알림이_아니면_읽음처리시_FORBIDDEN() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification(1L, 99L, false)));

        assertThatThrownBy(() -> notificationService().markRead(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 존재하지_않는_알림이면_NOTIFICATION_NOT_FOUND() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService().markRead(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 본인_알림이면_읽음처리된다() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification(1L, 1L, false)));

        var response = notificationService().markRead(1L, 1L);

        assertThat(response.read()).isTrue();
    }

    @Test
    void 전체_읽음처리는_리포지토리에_위임한다() {
        when(notificationRepository.markAllAsRead(eq(1L), any(LocalDateTime.class))).thenReturn(3);

        int updated = notificationService().markAllRead(1L);

        assertThat(updated).isEqualTo(3);
    }
}
