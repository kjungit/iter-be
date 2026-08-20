package com.example.iter.auth.controller.api.spec;

import com.example.iter.auth.dto.request.AddressUpdateRequest;
import com.example.iter.auth.dto.request.PasswordChangeRequest;
import com.example.iter.auth.dto.request.UserDeleteRequest;
import com.example.iter.auth.dto.request.UserUpdateRequest;
import com.example.iter.auth.dto.response.AddressResponse;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "마이페이지 API")
@SecurityRequirement(name = "JWT")
public interface UserApiSpec {

    @Operation(summary = "내 정보 조회")
    ResponseEntity<UserResponse> getMe(@Parameter(hidden = true) CustomUserDetails principal);

    @Operation(summary = "내 정보 수정", description = "요청에 포함된 이름, 닉네임, 연락처만 수정합니다.")
    ResponseEntity<UserResponse> updateMe(
            @Parameter(hidden = true) CustomUserDetails principal,
            UserUpdateRequest request
    );

    @Operation(summary = "비밀번호 변경", description = "변경 성공 시 모든 Refresh Token을 폐기합니다.")
    ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) CustomUserDetails principal,
            PasswordChangeRequest request
    );

    @Operation(summary = "내 기본 배송지 조회")
    ResponseEntity<AddressResponse> getDefaultAddress(
            @Parameter(hidden = true) CustomUserDetails principal
    );

    @Operation(summary = "내 기본 배송지 수정",
            description = "기본 배송지가 없으면 새로 생성하고, 있으면 기존 배송지를 수정합니다.")
    ResponseEntity<AddressResponse> updateDefaultAddress(
            @Parameter(hidden = true) CustomUserDetails principal,
            AddressUpdateRequest request
    );

    @Operation(summary = "회원 탈퇴",
            description = "진행 중인 대여가 없을 때 회원과 소유 장비를 소프트 삭제하고 모든 Refresh Token을 폐기합니다.")
    ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) CustomUserDetails principal,
            UserDeleteRequest request
    );
}
