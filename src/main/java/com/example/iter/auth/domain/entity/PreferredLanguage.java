package com.example.iter.auth.domain.entity;

import java.util.Locale;

// 회원이 이메일 등 백엔드가 직접 렌더링해서 보내야 하는 콘텐츠를 어떤 언어로 받을지 결정한다.
// API 응답/알림 params는 프론트가 자체 i18n으로 처리하므로 이 값과 무관하다 — 이건 오직
// "백엔드가 그 자리에서 언어를 확정해서 보내야 하는" 경로(현재는 이메일)에만 쓰인다.
public enum PreferredLanguage {
    KO,
    EN;

    // 회원가입 시점에 Accept-Language로 초기값을 정할 때 쓴다. 그 이후로는 사용자가 설정에서
    // 직접 바꾸기 전까진 이 값을 다시 덮어쓰지 않는다 — 매 요청마다 갱신하면 사용자가 설정한 값을
    // 조용히 되돌려버릴 수 있어서, 최초 1회(가입 시점)에만 감지해서 기본값으로 저장하는 방식을 쓴다.
    public static PreferredLanguage fromLocale(Locale locale) {
        if (locale != null && Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            return EN;
        }
        return KO;
    }
}
