package com.example.iter.auth.dto.request;

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

    public String name() {
        return name;
    }

    public String nickname() {
        return nickname;
    }

    public String phone() {
        return phone;
    }

    @AssertTrue(message = "변경할 회원 정보를 하나 이상 입력해주세요.")
    public boolean isAnyFieldPresent() {
        return name != null || nickname != null || phone != null;
    }
}
