package com.example.iter.common.config;

import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.response.ErrorResponse;
import com.example.iter.common.security.JwtAuthenticationFilter;
import com.example.iter.auth.support.KakaoOAuth2FailureHandler;
import com.example.iter.auth.support.KakaoOAuth2SuccessHandler;
import com.example.iter.auth.support.NoOpOAuth2AuthorizedClientRepository;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

// REST API 전용 Stateless 시큐리티 설정.
// (기획서 DoD "JWT 기반 인증/인가 기능 정상 동작 (Role + 소유권 기반)" 대응)
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 소유권 기반 인가(@PreAuthorize 커스텀 표현식)를 위해 필요
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonMapper jsonMapper;
    private final CorsProperties corsProperties;
    private final KakaoOAuth2SuccessHandler kakaoOAuth2SuccessHandler;
    private final KakaoOAuth2FailureHandler kakaoOAuth2FailureHandler;
    private final NoOpOAuth2AuthorizedClientRepository noOpOAuth2AuthorizedClientRepository;

    // 인증 없이 접근 가능한 경로.
    private static final String[] PERMIT_ALL_PATHS = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/csrf",
            "/api/v1/auth/oauth2/kakao/exchange",
            "/api/v1/auth/oauth2/kakao/signup",

            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",

            "/error"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizedClientRepository(noOpOAuth2AuthorizedClientRepository)
                        .successHandler(kakaoOAuth2SuccessHandler)
                        .failureHandler(kakaoOAuth2FailureHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(new OrRequestMatcher(
                                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/refresh"),
                                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/logout"),
                                PathPatternRequestMatcher.pathPattern(
                                        HttpMethod.POST,
                                        "/api/v1/auth/oauth2/kakao/exchange"
                                ),
                                PathPatternRequestMatcher.pathPattern(
                                        HttpMethod.POST,
                                        "/api/v1/auth/oauth2/kakao/signup"
                                )
                        )))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/devices",
                                "/api/v1/devices/*",
                                "/api/v1/devices/*/availability",
                                "/api/v1/devices/*/estimate"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint()) // 401 - 인증 안 됨
                        .accessDeniedHandler(accessDeniedHandler())           // 403 - 권한/소유권 없음
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    // ADMIN은 USER 권한도 포함 (관리자가 일반 회원용 API도 호출 가능하도록)
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("USER")
                .build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeErrorResponse(response, ErrorCode.FORBIDDEN);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(
                ErrorResponse.from(errorCode.name(), errorCode.getMessage())
        ));
    }
}
