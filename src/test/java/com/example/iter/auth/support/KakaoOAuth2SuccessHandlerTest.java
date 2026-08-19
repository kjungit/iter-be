package com.example.iter.auth.support;

import com.example.iter.auth.domain.repository.OAuthPendingTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class KakaoOAuth2SuccessHandlerTest {

    @Autowired
    private KakaoOAuth2SuccessHandler successHandler;

    @Autowired
    private OAuthPendingTokenRepository pendingTokenRepository;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        pendingTokenRepository.deleteAll();
    }

    @Test
    void callbackStoresExchangeCodeOnlyInSessionAndRedirectsWithoutQuery() throws Exception {
        Map<String, Object> attributes = Map.of(
                "id", 987654321L,
                "kakao_account", Map.of(
                        "email", "callback@example.com",
                        "profile", Map.of("nickname", "콜백회원")
                )
        );
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "kakao"
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/oauth2/callback")
                .doesNotContain("code=");
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(OAuth2ExchangeSessionManager.ATTRIBUTE_NAME))
                .isInstanceOf(String.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(pendingTokenRepository.count()).isOne();
    }

    @Test
    void applicationUsesNoOpAuthorizedClientRepository() {
        assertThat(authorizedClientRepository)
                .isInstanceOf(NoOpOAuth2AuthorizedClientRepository.class);
    }
}
