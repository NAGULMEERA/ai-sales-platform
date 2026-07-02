package com.aisales.common.observability.http;

import java.util.Locale;

/**
 * Shared diagnostics for outbound HTTP/SMTP/RPC calls made by any service (Rule 08/09
 * resilience: every timeout must be observable with correlation_id, trace_id, target service,
 * and elapsed time; correlation_id/trace_id/tenant_id are already carried automatically via MDC,
 * this utility supplies the remaining call-site-specific facts).
 *
 * <p>Deliberately provider-agnostic: {@code RestTemplate}, {@code RestClient} and JavaMail can
 * each be backed by a different underlying implementation (JDK HttpClient, Apache HttpClient 5,
 * plain sockets), and each surfaces a timeout as a different concrete exception type
 * ({@code java.net.SocketTimeoutException}, {@code org.apache.hc.client5.http.ConnectTimeoutException},
 * {@code java.net.http.HttpTimeoutException}, ...). Matching on the substring "timeout" in the
 * exception class name across the full cause chain avoids hard-coding a dependency on any one of
 * those optional libraries while still reliably classifying the failure.
 */
public final class OutboundCallDiagnostics {

    private OutboundCallDiagnostics() {
    }

    public static boolean isTimeout(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public static String outcome(Throwable ex) {
        return isTimeout(ex) ? "timeout" : "error";
    }

    public static long elapsedMillisSince(long startedAtEpochMillis) {
        return System.currentTimeMillis() - startedAtEpochMillis;
    }
}
