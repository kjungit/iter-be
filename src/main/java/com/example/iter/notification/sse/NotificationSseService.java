package com.example.iter.notification.sse;

import com.example.iter.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    private final SseEmitterRegistry registry;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = registry.register(userId);
        // 연결 직후 더미 이벤트를 하나 보내야 일부 프록시/브라우저가 커넥션을 idle로 보고 끊는 걸 방지할 수 있다.
        sendToEmitter(userId, emitter, "connect", "connected");
        return emitter;
    }

    public void send(Long userId, NotificationResponse payload) {
        for (SseEmitter emitter : registry.get(userId)) {
            sendToEmitter(userId, emitter, "notification", payload);
        }
    }

    private void sendToEmitter(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            // IOException(끊긴 연결에 쓰기 시도) 외에, emitter가 이미 완료된 상태에서 send()를 호출하면
            // IllegalStateException(unchecked)도 던진다 — 둘 다 "이 emitter는 이제 못 쓴다"는 같은 신호라
            // 함께 잡아서 여기서 끝낸다. 여기서 벗어나면 NotificationService.create()까지 전파되어
            // 이미 저장된 Notification 행까지 롤백시킬 수 있다.
            log.debug("SSE emitter 전송 실패로 연결 종료: userId={}", userId);
            registry.remove(userId, emitter);
            emitter.completeWithError(e);
        }
    }
}
