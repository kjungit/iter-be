package com.example.iter.auth.support;

import com.example.iter.auth.config.RefreshTokenCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieManager {

    private final RefreshTokenCookieProperties properties;

    public ResponseCookie create(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(properties.getMaxAge())
                .build();
    }

    public ResponseCookie delete() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<String> extract(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> properties.getName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public String getCookieName() {
        return properties.getName();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.getName(), value)
                .httpOnly(properties.isHttpOnly())
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(properties.getPath());
    }
}
