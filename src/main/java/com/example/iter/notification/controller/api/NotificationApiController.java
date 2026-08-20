package com.example.iter.notification.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.notification.dto.response.MarkAllReadResponse;
import com.example.iter.notification.dto.response.NotificationResponse;
import com.example.iter.notification.dto.response.UnreadCountResponse;
import com.example.iter.notification.service.NotificationService;
import com.example.iter.notification.sse.NotificationSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;

    @Operation(summary = "실시간 알림 구독 (SSE)", security = @SecurityRequirement(name = "JWT"))
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails principal) {
        return notificationSseService.subscribe(principal.getUser().getId());
    }

    @Operation(summary = "알림 목록 조회", security = @SecurityRequirement(name = "JWT"))
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
                                                                               ) {
        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                principal.getUser().getId(), unreadOnly, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "읽지 않은 알림 개수", security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
                                                               ) {
        long count = notificationService.getUnreadCount(principal.getUser().getId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @Operation(summary = "알림 읽음 처리", security = @SecurityRequirement(name = "JWT"))
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long notificationId
                                                          ) {
        NotificationResponse response = notificationService.markRead(principal.getUser().getId(), notificationId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 전체 읽음 처리", security = @SecurityRequirement(name = "JWT"))
    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllRead(
            @AuthenticationPrincipal CustomUserDetails principal
                                                            ) {
        int updated = notificationService.markAllRead(principal.getUser().getId());
        return ResponseEntity.ok(new MarkAllReadResponse(updated));
    }
}
