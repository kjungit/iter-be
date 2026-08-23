package com.example.iter.notification.service;

import com.example.iter.common.dto.response.PageResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;
    private final MailService mailService;
    private final NotificationProperties notificationProperties;

    // NotificationEventListener가 이벤트당 여러 번(owner/renter 각각) 호출한다.
    // 알림 저장은 성공했는데 SSE/메일만 실패하는 경우를 막기 위해, DB 저장 이후 실패 가능성이 있는 두 side effect는 각자 내부에서 예외를 삼키도록 구현
    // (NotificationSseService, JavaMailService).
    //
    // REQUIRES_NEW로 강제하는 이유: 이 메서드는 항상 @TransactionalEventListener(AFTER_COMMIT) 콜백에서 호출된다
    // — 원본 트랜잭션이 막 커밋된 직후라 기본 REQUIRED로는 새 트랜잭션이 제대로 시작되지 않고 조용히 아무 것도 커밋되지 않는 경우가 있다
    // (예외도 안 던져서 알아채기 어렵다)
    // REQUIRES_NEW로 독립된 트랜잭션을 확실히 새로 열어야 함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long receiverId, String receiverEmail, NotificationType type,
                        String title, String message, Long rentalId) {
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .receiverId(receiverId)
                        .type(type)
                        .title(title)
                        .message(message)
                        .rentalId(rentalId)
                        .build()
        );

        notificationSseService.send(receiverId, NotificationResponse.from(notification));

        if (type.requiresEmail()) {
            // mailService.send()는 @Async라 실제 발송 실패는 그 메서드 안에서 잡히지만 스레드풀 큐가 가득 차서 작업 "제출" 자체가 거부되면 RejectedExecutionException이 이 호출부에서 동기적으로 튀어나온다
            // — 여기서도 못 잡으면 방금 저장한 Notification까지 롤백된다.
            try {
                mailService.send(new MailMessage(receiverEmail, notificationProperties.mailFrom(), title, message));
            } catch (Exception e) {
                log.warn("메일 발송 요청 실패: to={}", receiverEmail, e);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByReceiverIdAndReadFalseOrderByCreatedAtDescIdDesc(userId, pageable)
                : notificationRepository.findByReceiverIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return PageResponse.from(result.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isReceiver(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
}
