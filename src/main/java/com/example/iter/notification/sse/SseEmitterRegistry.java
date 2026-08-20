package com.example.iter.notification.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

// SSE 구독 연결을 사용자별로 보관/조회하는 저장소.
// 지금은 인메모리 구현체(InMemorySseEmitterRegistry) 하나만 쓰지만 나중에 여러 서버 인스턴스로함 스케일아웃하면 이 인터페이스를 유지한 채 Redis Pub/Sub 기반 구현체로 교체할 수 있도록 분리
public interface SseEmitterRegistry {

    SseEmitter register(Long userId);

    void remove(Long userId, SseEmitter emitter);

    List<SseEmitter> get(Long userId);
}
