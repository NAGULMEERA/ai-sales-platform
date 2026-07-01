package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.OAuthAccount;
import com.aisales.identity.domain.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
