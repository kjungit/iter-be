package com.example.iter.auth.support;

import com.example.iter.auth.service.model.KakaoUserInfo;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KakaoOAuth2UserInfoMapper {

    public KakaoUserInfo map(OAuth2User oAuth2User) {
        Object id = oAuth2User.getAttribute("id");
        if (id == null) {
            throw new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }

        Map<String, Object> account = attributeMap(oAuth2User.getAttribute("kakao_account"));
        Map<String, Object> profile = attributeMap(account.get("profile"));

        return new KakaoUserInfo(
                String.valueOf(id),
                stringValue(account.get("email")),
                stringValue(profile.get("nickname"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> attributeMap(Object attribute) {
        return attribute instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
