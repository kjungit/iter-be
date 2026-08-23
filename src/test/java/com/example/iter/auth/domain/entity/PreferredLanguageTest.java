package com.example.iter.auth.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PreferredLanguageTest {

    @Test
    void 영어_로케일이면_EN을_반환한다() {
        assertThat(PreferredLanguage.fromLocale(Locale.ENGLISH)).isEqualTo(PreferredLanguage.EN);
        assertThat(PreferredLanguage.fromLocale(Locale.US)).isEqualTo(PreferredLanguage.EN);
    }

    @Test
    void 한국어_로케일이면_KO를_반환한다() {
        assertThat(PreferredLanguage.fromLocale(Locale.KOREAN)).isEqualTo(PreferredLanguage.KO);
    }

    @Test
    void 지원하지_않는_로케일이면_KO로_기본값_처리한다() {
        assertThat(PreferredLanguage.fromLocale(Locale.JAPANESE)).isEqualTo(PreferredLanguage.KO);
    }

    @Test
    void locale이_null이면_KO로_기본값_처리한다() {
        assertThat(PreferredLanguage.fromLocale(null)).isEqualTo(PreferredLanguage.KO);
    }
}
