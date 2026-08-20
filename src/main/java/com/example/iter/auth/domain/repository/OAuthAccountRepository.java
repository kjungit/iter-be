package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.OAuthAccount;
import com.example.iter.auth.domain.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );

    boolean existsByUserIdAndProvider(Long userId, OAuthProvider provider);
}
