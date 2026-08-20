package com.example.iter.notification.sse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SseTicketServiceTest {

    private final SseTicketService sseTicketService = new SseTicketService();

    @Test
    void 발급한_티켓으로_사용자_id를_조회할_수_있다() {
        String ticket = sseTicketService.issue(42L);

        Optional<Long> resolved = sseTicketService.consume(ticket);

        assertThat(resolved).contains(42L);
    }

    @Test
    void 티켓은_한_번_쓰면_재사용할_수_없다() {
        String ticket = sseTicketService.issue(42L);
        sseTicketService.consume(ticket);

        Optional<Long> secondAttempt = sseTicketService.consume(ticket);

        assertThat(secondAttempt).isEmpty();
    }

    @Test
    void 존재하지_않는_티켓은_빈_값을_반환한다() {
        assertThat(sseTicketService.consume("no-such-ticket")).isEmpty();
    }

    @Test
    void 만료된_티켓은_소모해도_빈_값을_반환한다() throws InterruptedException {
        SseTicketService shortLived = new SseTicketService(Duration.ofMillis(1));
        String ticket = shortLived.issue(42L);
        Thread.sleep(10);

        assertThat(shortLived.consume(ticket)).isEmpty();
    }
}
