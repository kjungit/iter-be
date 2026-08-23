package com.example.iter.auth.dto.request;

import com.example.iter.auth.domain.entity.PreferredLanguage;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

        @Size(min = 1, max = 20, message = "이름은 1자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "이름은 공백만 입력할 수 없습니다.")
        private String name;

        @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "닉네임은 공백만 입력할 수 없습니다.")
        private String nickname;

        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        private String phone;

        // 이메일 등 백엔드가 직접 언어를 확정해서 보내야 하는 콘텐츠(현재는 이메일)에 쓰인다.
        private PreferredLanguage preferredLanguage;

    @JsonSetter(value = "name", nulls = Nulls.FAIL)
    public void setName(String name) {
        this.name = name.trim();
    }

    @JsonSetter(value = "nickname", nulls = Nulls.FAIL)
    public void setNickname(String nickname) {
        this.nickname = nickname.trim();
    }

    @JsonSetter(value = "phone", nulls = Nulls.FAIL)
    public void setPhone(String phone) {
        this.phone = phone.trim();
    }

    @JsonSetter(value = "preferredLanguage", nulls = Nulls.FAIL)
    public void setPreferredLanguage(PreferredLanguage preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String name() {
        return name;
    }

    public String nickname() {
        return nickname;
    }

    public String phone() {
        return phone;
    }

    public PreferredLanguage preferredLanguage() {
        return preferredLanguage;
    }

    @AssertTrue(message = "변경할 회원 정보를 하나 이상 입력해주세요.")
    public boolean isAnyFieldPresent() {
        return name != null || nickname != null || phone != null || preferredLanguage != null;
    }
}
