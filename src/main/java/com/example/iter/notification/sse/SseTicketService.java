package com.example.iter.notification.sse;

import com.example.iter.common.security.SseTicketResolver;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// EventSource가 Authorization 헤더를 못 실어 보내는 문제를, 액세스 토큰 원문 대신
// "이 SSE 연결 하나만" 열 수 있는 단발성·초단기 티켓으로 우회한다.
// 액세스 토큰(전체 API 권한, 15분 유효)이 URL/로그에 남는 것과 비교해, 이 티켓은 유출돼도
// SSE 구독 한 번 외엔 아무것도 못 하고 60초 안에, 혹은 쓰이는 즉시 무효화된다.
@Component
public class SseTicketService implements SseTicketResolver {

    private static final int MAX_TRACKED_TICKETS = 10_000; // 발급만 하고 안 쓴 티켓이 무한정 쌓이는 걸 막는 상한선
    private static final Duration DEFAULT_TICKET_VALIDITY = Duration.ofSeconds(60);

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final Duration ticketValidity;

    public SseTicketService() {
        this(DEFAULT_TICKET_VALIDITY);
    }

    // 테스트에서 만료 동작을 실제로 60초 기다리지 않고 검증할 수 있도록 열어둔 생성자.
    SseTicketService(Duration ticketValidity) {
        this.ticketValidity = ticketValidity;
    }

    public String issue(Long userId) {
        sweepExpiredIfCrowded();
        String value = UUID.randomUUID().toString();
        tickets.put(value, new Ticket(userId, Instant.now().plus(ticketValidity)));
        return value;
    }

    @Override
    public Optional<Long> consume(String ticket) {
        if (ticket == null) {
            return Optional.empty();
        }
        Ticket found = tickets.remove(ticket); // 조회와 동시에 제거 -> 재사용(replay) 불가
        if (found == null || found.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(found.userId());
    }

    // 별도 스케줄러 없이, 티켓이 너무 많이 쌓였을 때만 이번 발급 요청에 얹혀서 청소한다.
    private void sweepExpiredIfCrowded() {
        if (tickets.size() < MAX_TRACKED_TICKETS) {
            return;
        }
        tickets.values().removeIf(Ticket::isExpired);
    }

    private record Ticket(Long userId, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
