package com.example.iter.notification.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.mail.MailMessage;
import com.example.iter.common.mail.MailService;
import com.example.iter.notification.config.NotificationProperties;
import com.example.iter.notification.domain.entity.Notification;
import com.example.iter.notification.domain.entity.NotificationType;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import com.example.iter.notification.domain.repository.NotificationRepository;
import com.example.iter.notification.dto.response.NotificationResponse;
import com.example.iter.notification.sse.NotificationSseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        return notification(id, receiverId, read, LocalDateTime.now());
    }

    private Notification notification(Long id, Long receiverId, boolean read, LocalDateTime createdAt) {
        Notification notification = Notification.builder()
                .id(id)
                .receiverId(receiverId)
                .type(NotificationType.RENTAL_APPROVED)
                .params(Map.of("productName", "테스트 장비"))
                .rentalId(10L)
                .read(read)
                .build();
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notification;
    }

    @Test
    void 메일이_필요한_타입이면_SSE와_메일을_모두_보낸다() {
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService().create(1L, "owner@test.com", NotificationType.RENTAL_REQUESTED,
                "새로운 대여 신청", "메시지", Map.of("productName", "테스트 장비"), 10L);

        verify(notificationSseService).send(eq(1L), any(NotificationResponse.class));
        verify(mailService).send(new MailMessage("owner@test.com", "noreply@iter.example.com", "새로운 대여 신청", "메시지"));
    }

    @Test
    void 메일이_필요없는_타입이면_SSE만_보낸다() {
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService().create(2L, "renter@test.com", NotificationType.PAYMENT_COMPLETED_RENTER,
                "결제 완료", "메시지", Map.of("productName", "테스트 장비"), 10L);

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

    @Test
    void 다음_페이지가_있으면_size_1개를_더_받아_hasNext와_nextCursor를_계산한다() {
        // size(2)보다 1개 더 많은 3개를 리포지토리가 돌려주면, 마지막 1개는 잘라내고 hasNext=true여야 한다.
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 10, 0);
        Notification first = notification(30L, 1L, false, now);
        Notification second = notification(20L, 1L, false, now.minusMinutes(1));
        Notification third = notification(10L, 1L, false, now.minusMinutes(2));
        when(notificationRepository.findNextByReceiverId(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(first, second, third));

        var response = notificationService().getNotifications(1L, false, null, 2);

        assertThat(response.content()).extracting(NotificationResponse::id).containsExactly(30L, 20L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor())
                .isEqualTo(CursorCodec.encode(new CursorKey(second.getCreatedAt(), second.getId())));
    }

    @Test
    void 남은_알림이_요청_size_이하면_hasNext는_false이고_nextCursor는_없다() {
        LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 8, 23, 9, 0);
        String cursor = CursorCodec.encode(new CursorKey(cursorCreatedAt, 20L));
        Notification only = notification(10L, 1L, false, cursorCreatedAt.minusMinutes(1));
        when(notificationRepository.findNextByReceiverId(eq(1L), eq(cursorCreatedAt), eq(20L), any(Pageable.class)))
                .thenReturn(List.of(only));

        var response = notificationService().getNotifications(1L, false, cursor, 5);

        assertThat(response.content()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void unreadOnly가_true면_읽지_않은_알림_전용_조회_메서드를_쓴다() {
        when(notificationRepository.findNextUnreadByReceiverId(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(notification(10L, 1L, false)));

        notificationService().getNotifications(1L, true, null, 20);

        verify(notificationRepository).findNextUnreadByReceiverId(eq(1L), isNull(), isNull(), any(Pageable.class));
        verify(notificationRepository, never()).findNextByReceiverId(any(), any(), any(), any());
    }
}
