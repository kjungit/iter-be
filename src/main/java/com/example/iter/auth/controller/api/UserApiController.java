package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.request.AddressUpdateRequest;
import com.example.iter.auth.dto.response.AddressResponse;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.auth.service.UserAddressService;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "마이페이지 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserAddressService userAddressService;

    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(UserResponse.from(principal.getUser()));
    }

    @Operation(summary = "내 기본 배송지 조회", security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me/address")
    public ResponseEntity<AddressResponse> getDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userAddressService.getDefaultAddress(principal.getUser().getId()));
    }

    @Operation(
            summary = "내 기본 배송지 수정",
            description = "기본 배송지가 없으면 새로 생성하고, 있으면 기존 배송지를 수정합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PutMapping("/me/address")
    public ResponseEntity<AddressResponse> updateDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddressUpdateRequest request
    ) {
        return ResponseEntity.ok(
                userAddressService.updateDefaultAddress(principal.getUser().getId(), request)
        );
    }
}
