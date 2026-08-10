package com.example.iter.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

// application.yml의 jwt.* 프로퍼티를 바인딩하는 클래스 (실제 값은 application-secret.yml에서 채움)
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;
    private String secretKey;
    private Duration accessTokenValidity;
    private Duration refreshTokenValidity;
}
