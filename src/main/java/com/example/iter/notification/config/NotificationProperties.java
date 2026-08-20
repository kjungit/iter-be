package com.example.iter.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// redisChannelPrefix는 지금은 사용하지 않지만 나중에 SseEmitterRegistry를 Redis Pub/Sub 구현체로 교체할 때 채널 키(notification:user:{userId}) 규칙을 코드 여기저기 흩어놓지 않기 위해 미리 정의
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(String mailFrom, String redisChannelPrefix) {

    public String channelKey(Long userId) {
        return redisChannelPrefix + userId;
    }
}
