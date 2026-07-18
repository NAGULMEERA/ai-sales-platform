package com.aisales.integration.infrastructure.persistence;

import com.aisales.integration.domain.entity.VoiceCall;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceCallRepository extends JpaRepository<VoiceCall, UUID> {

    Optional<VoiceCall> findByProviderAndProviderCallId(String provider, String providerCallId);
}
