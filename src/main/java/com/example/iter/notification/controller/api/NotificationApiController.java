package com.example.iter.notification.controller.api;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.notification.dto.response.MarkAllReadResponse;
import com.example.iter.notification.dto.response.NotificationResponse;
import com.example.iter.notification.dto.response.SseTicketResponse;
import com.example.iter.notification.dto.response.UnreadCountResponse;
import com.example.iter.notification.controller.api.spec.NotificationApiSpec;
import com.example.iter.notification.service.NotificationService;
import com.example.iter.notification.sse.NotificationSseService;
import com.example.iter.notification.sse.SseTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController implements NotificationApiSpec {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final SseTicketService sseTicketService;

    @Override
    @PostMapping("/sse-ticket")
    public ResponseEntity<SseTicketResponse> issueSseTicket(@AuthenticationPrincipal CustomUserDetails principal) {
        String ticket = sseTicketService.issue(principal.getUser().getId());
        return ResponseEntity.ok(new SseTicketResponse(ticket));
    }

    @Override
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails principal) {
        return notificationSseService.subscribe(principal.getUser().getId());
    }

    @Override
    @GetMapping
    public ResponseEntity<CursorPageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<NotificationResponse> response = notificationService.getNotifications(
                principal.getUser().getId(), unreadOnly, cursor, size);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        long count = notificationService.getUnreadCount(principal.getUser().getId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        NotificationResponse response = notificationService.markRead(principal.getUser().getId(), notificationId);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllRead(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        int updated = notificationService.markAllRead(principal.getUser().getId());
        return ResponseEntity.ok(new MarkAllReadResponse(updated));
    }
}
