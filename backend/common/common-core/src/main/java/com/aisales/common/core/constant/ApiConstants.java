package com.aisales.common.core.constant;

public final class ApiConstants {

    public static final String API_V1 = "/api/v1";
    public static final String API_V2 = "/api/v2";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String DEPRECATION_HEADER = "Deprecation";
    public static final String SUNSET_HEADER = "Sunset";
    public static final String LINK_HEADER = "Link";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_PAGE_NUMBER = "0";

    /** Minimum concurrent API versions supported. */
    public static final int MIN_SUPPORTED_VERSIONS = 2;

    /** Months of notice before sunsetting a deprecated version. */
    public static final int DEPRECATION_NOTICE_MONTHS = 6;

    private ApiConstants() {
    }
}
