package com.example.iter.auth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public class RefreshTokenCookieProperties {

    @NotBlank
    private String name;

    private boolean secure;
    private boolean httpOnly;

    @NotBlank
    private String sameSite;

    @NotBlank
    private String path;

    @NotNull
    private Duration maxAge;

    @AssertTrue(message = "__Secure- 접두사가 붙은 Cookie는 secure=true여야 합니다.")
    public boolean isSecurePrefixValid() {
        return name == null || !name.startsWith("__Secure-") || secure;
    }
}
