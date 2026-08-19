package com.example.iter.auth.support;

import com.example.iter.auth.service.model.KakaoUserInfo;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoOAuth2UserInfoMapperTest {

    private final KakaoOAuth2UserInfoMapper mapper = new KakaoOAuth2UserInfoMapper();

    @Test
    void mapsKakaoIdAndOptionalProfileAttributes() {
        DefaultOAuth2User oAuth2User = user(Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "kakao@example.com",
                        "profile", Map.of("nickname", "카카오닉네임")
                )
        ));

        KakaoUserInfo userInfo = mapper.map(oAuth2User);

        assertThat(userInfo.providerUserId()).isEqualTo("123456789");
        assertThat(userInfo.email()).isEqualTo("kakao@example.com");
        assertThat(userInfo.nickname()).isEqualTo("카카오닉네임");
    }

    @Test
    void allowsMissingOptionalKakaoAttributes() {
        KakaoUserInfo userInfo = mapper.map(user(Map.of("id", 123L)));

        assertThat(userInfo.email()).isNull();
        assertThat(userInfo.nickname()).isNull();
    }

    @Test
    void rejectsResponseWithoutProviderUserId() {
        assertThatThrownBy(() -> mapper.map(user(Map.of("name", "invalid"))))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.OAUTH_AUTHENTICATION_FAILED));
    }

    private DefaultOAuth2User user(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                attributes.containsKey("id") ? "id" : "name"
        );
    }
}
