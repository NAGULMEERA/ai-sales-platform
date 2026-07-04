package com.aisales.identity.oauth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aisales.identity.oauth.domain.entity.OAuthAccount;
import com.aisales.identity.oauth.domain.enums.OAuthProvider;



public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
