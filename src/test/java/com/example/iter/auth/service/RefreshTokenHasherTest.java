package com.example.iter.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher hasher = new RefreshTokenHasher();

    @Test
    void hashesTokenDeterministicallyWithoutStoringTheOriginal() {
        String rawToken = "header.payload.signature";

        String firstHash = hasher.hash(rawToken);
        String secondHash = hasher.hash(rawToken);

        assertThat(firstHash)
                .isEqualTo(secondHash)
                .hasSize(43)
                .doesNotContain(rawToken);
    }

    @Test
    void differentTokensHaveDifferentHashes() {
        assertThat(hasher.hash("refresh-token-a"))
                .isNotEqualTo(hasher.hash("refresh-token-b"));
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> hasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
