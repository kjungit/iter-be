package com.example.iter.auth.controller.api.spec;

import com.example.iter.auth.dto.request.KakaoSignUpRequest;
import com.example.iter.auth.dto.response.AccessTokenResponse;
import com.example.iter.auth.dto.response.OAuthActionRequiredResponse;
import com.example.iter.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

@Tag(name = "OAuth2 Auth", description = "카카오 OAuth2 로그인·회원가입 API")
public interface OAuth2AuthApiSpec {

    @Operation(
            summary = "카카오 로그인 교환",
            description = "OAuth2 임시 세션을 로그인 결과 또는 추가 절차 토큰으로 교환합니다. 요청 Body는 없습니다."
    )
    @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "XSRF-TOKEN Cookie와 동일한 CSRF Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카카오 계정 로그인 성공",
                    content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
            @ApiResponse(responseCode = "202", description = "회원가입 또는 기존 계정 연결 필요",
                    content = @Content(schema = @Schema(implementation = OAuthActionRequiredResponse.class))),
            @ApiResponse(responseCode = "401", description = "OAuth 임시 세션 또는 일회용 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "CSRF Token이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<?> exchange(@Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "카카오 신규 회원가입",
            description = "카카오 신규 회원의 추가 정보를 저장하고 즉시 로그인 처리합니다. "
                    + "입력한 이메일로 이미 가입된 계정이 있으면 가입 대신 계정 연결 절차(LINK_REQUIRED)로 전환됩니다.")
    @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "XSRF-TOKEN Cookie와 동일한 CSRF Token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "카카오 신규 회원가입 및 로그인 성공",
                    content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
            @ApiResponse(responseCode = "202", description = "입력한 이메일로 이미 가입된 계정이 있어 계정 연결 절차 필요",
                    content = @Content(schema = @Schema(implementation = OAuthActionRequiredResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 또는 카카오 이메일 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "OAuth 일회용 토큰이 유효하지 않거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "CSRF Token이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 연결된 카카오 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<?> signUp(KakaoSignUpRequest request, @Parameter(hidden = true) Locale locale);
}
