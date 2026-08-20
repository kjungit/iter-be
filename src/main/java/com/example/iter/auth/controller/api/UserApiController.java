package com.example.iter.auth.controller.api;

import com.example.iter.auth.controller.api.spec.UserApiSpec;
import com.example.iter.auth.dto.request.AddressUpdateRequest;
import com.example.iter.auth.dto.request.PasswordChangeRequest;
import com.example.iter.auth.dto.request.UserDeleteRequest;
import com.example.iter.auth.dto.request.UserUpdateRequest;
import com.example.iter.auth.dto.response.AddressResponse;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.auth.service.UserAccountService;
import com.example.iter.auth.service.UserAddressService;
import com.example.iter.auth.support.RefreshTokenCookieManager;
import com.example.iter.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController implements UserApiSpec {

    private final UserAddressService userAddressService;
    private final UserAccountService userAccountService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @Override
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userAccountService.getMyProfile(principal.getUser().getId()));
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(
                userAccountService.updateMyProfile(principal.getUser().getId(), request));
    }

    @Override
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        userAccountService.changePassword(principal.getUser().getId(), request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/me/address")
    public ResponseEntity<AddressResponse> getDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userAddressService.getDefaultAddress(principal.getUser().getId()));
    }

    @Override
    @PutMapping("/me/address")
    public ResponseEntity<AddressResponse> updateDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddressUpdateRequest request
    ) {
        return ResponseEntity.ok(
                userAddressService.updateDefaultAddress(principal.getUser().getId(), request)
        );
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody(required = false) UserDeleteRequest request
    ) {
        userAccountService.withdraw(principal.getUser().getId(), request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.delete().toString())
                .build();
    }
}
