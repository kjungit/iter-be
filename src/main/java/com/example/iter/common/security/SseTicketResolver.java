package com.example.iter.common.security;

import java.util.Optional;

// SSE 구독용 단발성 티켓을 사용자 id로 바꿔주는 창구.
// common/security는 특정 도메인을 모르게 유지해야 해서, 실제 티켓 발급/저장 로직은
// notification 쪽 구현체(SseTicketService)에 두고 이 인터페이스로만 의존한다.
public interface SseTicketResolver {

    // 티켓을 조회해서 유효하면 사용자 id를 반환하고, 그 즉시 티켓을 소모(재사용 불가)한다.
    Optional<Long> consume(String ticket);
}
