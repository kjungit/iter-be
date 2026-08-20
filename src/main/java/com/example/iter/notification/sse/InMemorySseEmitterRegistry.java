package com.example.iter.notification.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 단일 인스턴스 배포를 전제로 한 인메모리 구현체.
// 같은 사용자가 여러 탭/기기에서 접속할 수 있어 사용자당 emitter를 리스트로 보관한다.
@Component
public class InMemorySseEmitterRegistry implements SseEmitterRegistry {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30분 — 그 안에 프론트가 재연결한다고 가정

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));

        return emitter;
    }

    @Override
    public void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }

    @Override
    public List<SseEmitter> get(Long userId) {
        return emitters.getOrDefault(userId, List.of());
    }
}
