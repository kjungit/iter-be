package com.example.iter.notification.controller.api.spec;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.notification.dto.response.MarkAllReadResponse;
import com.example.iter.notification.dto.response.NotificationResponse;
import com.example.iter.notification.dto.response.SseTicketResponse;
import com.example.iter.notification.dto.response.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationApiSpec {

    @Operation(
            summary = "SSE 구독용 단발성 티켓 발급",
            description = "브라우저 EventSource가 Authorization 헤더를 못 보내는 문제를 우회하기 위한 티켓. "
                    + "60초 이내에 /subscribe에서 한 번만 쓸 수 있고, 쓰이는 즉시 무효화된다.",
            security = @SecurityRequirement(name = "JWT")
    )
    ResponseEntity<SseTicketResponse> issueSseTicket(@Parameter(hidden = true) CustomUserDetails principal);

    @Operation(summary = "실시간 알림 구독 (SSE)", description = "쿼리 파라미터로 /sse-ticket에서 발급받은 ticket을 전달한다.")
    SseEmitter subscribe(@Parameter(hidden = true) CustomUserDetails principal);

    @Operation(
            summary = "알림 목록 조회",
            description = "offset이 아닌 cursor(keyset) 방식이다. 첫 요청은 cursor 없이 보내고, "
                    + "응답의 nextCursor를 다음 요청의 cursor로 그대로 넘기면 이어서 조회된다. hasNext가 false면 더 없다는 뜻.",
            security = @SecurityRequirement(name = "JWT")
    )
    ResponseEntity<CursorPageResponse<NotificationResponse>> getNotifications(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "읽지 않은 알림만 조회") boolean unreadOnly,
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청이면 생략") String cursor,
            @Parameter(description = "페이지 크기") int size
    );

    @Operation(summary = "읽지 않은 알림 개수", security = @SecurityRequirement(name = "JWT"))
    ResponseEntity<UnreadCountResponse> getUnreadCount(@Parameter(hidden = true) CustomUserDetails principal);

    @Operation(summary = "알림 읽음 처리", security = @SecurityRequirement(name = "JWT"))
    ResponseEntity<NotificationResponse> markRead(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "알림 ID", example = "1", required = true) Long notificationId
    );

    @Operation(summary = "알림 전체 읽음 처리", security = @SecurityRequirement(name = "JWT"))
    ResponseEntity<MarkAllReadResponse> markAllRead(@Parameter(hidden = true) CustomUserDetails principal);
}
