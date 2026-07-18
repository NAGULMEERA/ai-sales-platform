package com.aisales.common.core.constant;

public final class SecurityConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String JWT_CLAIM_TENANT_ID = "tenantId";
    public static final String TENANT_ID_CLAIM = JWT_CLAIM_TENANT_ID;
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String JWT_CLAIM_ROLES = "roles";
    public static final String ROLES_CLAIM = JWT_CLAIM_ROLES;
    public static final String JWT_CLAIM_PERMISSIONS = "permissions";
    public static final String PERMISSIONS_CLAIM = JWT_CLAIM_PERMISSIONS;
    public static final String ORGANIZATION_ID_CLAIM = "organizationId";
    public static final String EMAIL_CLAIM = "email";
    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";
    public static final String[] PUBLIC_PATHS = {
            "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/api/v1/auth/**", "/api/v1/public/**",
            "/api/v1/payments/webhooks/**",
            "/api/v1/integrations/webhooks/**",
            "/.well-known/jwks.json", "/api/v1/.well-known/jwks.json",
            "/oauth2/**", "/login/oauth2/**"
    };

    private SecurityConstants() {
    }
}
