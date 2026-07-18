package com.aisales.identity.authentication.api.controller;

import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the platform JWT public key set so gateways and microservices can validate
 * RS256 access tokens without distributing secrets.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final PlatformRsaKeyProvider keyProvider;

    @GetMapping({"/.well-known/jwks.json", "/api/v1/.well-known/jwks.json"})
    public Map<String, Object> jwks() {
        return keyProvider.jwks();
    }
}
