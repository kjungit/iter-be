package com.example.iter.auth.support;

import com.example.iter.auth.config.RefreshTokenCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieManagerTest {

    @Test
    void createsProductionCookieWithSecureAttributes() {
        RefreshTokenCookieManager manager = new RefreshTokenCookieManager(properties(
                "__Secure-iter-refresh",
                true
        ));

        ResponseCookie cookie = manager.create("raw-refresh-token");

        assertThat(cookie.getName()).isEqualTo("__Secure-iter-refresh");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(14));
    }

    @Test
    void deletionCookieKeepsScopeAndExpiresImmediately() {
        RefreshTokenCookieManager manager = new RefreshTokenCookieManager(properties(
                "iter-refresh",
                false
        ));

        ResponseCookie cookie = manager.delete();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isZero();
    }

    private RefreshTokenCookieProperties properties(String name, boolean secure) {
        RefreshTokenCookieProperties properties = new RefreshTokenCookieProperties();
        properties.setName(name);
        properties.setSecure(secure);
        properties.setHttpOnly(true);
        properties.setSameSite("Lax");
        properties.setPath("/api/v1/auth");
        properties.setMaxAge(Duration.ofDays(14));
        return properties;
    }
}
