package com.aisales.common.observability.http;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.function.Predicate;

/**
 * Classifies outbound-call failures as transient (safe to retry) or non-transient (retrying
 * cannot help, so retrying would just waste time and risk duplicate side effects).
 *
 * <p>Referenced from {@code application.yml} via {@code resilience4j.retry.instances.*.
 * retry-exception-predicate} and {@code resilience4j.circuitbreaker.instances.*.
 * record-failure-predicate}, which Resilience4j instantiates by fully-qualified class name using
 * a no-arg constructor - it is <b>not</b> resolved as a Spring bean, so this class must remain
 * stateless and dependency-free.
 *
 * <p>Transient (retry):
 * <ul>
 *   <li>Connect/read timeouts and connection failures ({@link SocketTimeoutException},
 *       {@link ConnectException}, Spring's {@link ResourceAccessException} wrapper)</li>
 *   <li>5xx responses ({@link HttpServerErrorException}) - the downstream service is unhealthy,
 *       not the request</li>
 *   <li>429 Too Many Requests - the only 4xx worth retrying (Rule 09: AI/provider rate limits are
 *       an explicitly-called-out transient failure), always with backoff</li>
 * </ul>
 *
 * <p>Never retried: authentication failures (HTTP 401/403, and SMTP's
 * {@code MailAuthenticationException} - denied by the default {@code false} fall-through below,
 * since it matches none of the transient cases), and any other 4xx (validation/business errors,
 * e.g. "invalid recipient email") where the request itself is the problem and repeating it
 * verbatim would fail identically every time.
 */
public final class TransientFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        Throwable current = throwable;
        int guard = 0;
        while (current != null && guard++ < 8) {
            if (isTransient(current)) {
                return true;
            }
            Throwable cause = current.getCause();
            current = (cause == current) ? null : cause;
        }
        return false;
    }

    private boolean isTransient(Throwable t) {
        if (t instanceof SocketTimeoutException || t instanceof ConnectException) {
            return true;
        }
        if (t instanceof ResourceAccessException) {
            return true;
        }
        if (t instanceof HttpServerErrorException) {
            return true;
        }
        if (t instanceof HttpClientErrorException httpClientErrorException) {
            return httpClientErrorException.getStatusCode().value() == 429;
        }
        if (t instanceof HttpStatusCodeException httpStatusCodeException
                && httpStatusCodeException.getStatusCode().value() == 429) {
            return true;
        }
        return isTransientSmtp(t);
    }

    /**
     * SMTP classification without a hard dependency on spring-mail types (Resilience4j
     * instantiates this predicate by FQCN outside the Spring context).
     */
    private static boolean isTransientSmtp(Throwable t) {
        String className = t.getClass().getName();
        if (className.endsWith("MailAuthenticationException")) {
            return false;
        }
        if (className.endsWith("MailSendException")
                || className.endsWith("MessagingException")
                || className.contains("MailException")) {
            return looksLikeTransientSmtpMessage(t.getMessage());
        }
        return false;
    }

    private static boolean looksLikeTransientSmtpMessage(String message) {
        if (message == null || message.isBlank()) {
            // Unknown SMTP send failure — safer to retry than drop.
            return true;
        }
        String m = message.toLowerCase();
        if (m.contains("authentication")
                || m.contains("invalid addresses")
                || m.contains("unknown user")
                || m.contains("550")) {
            return false;
        }
        return m.contains("421")
                || m.contains("450")
                || m.contains("451")
                || m.contains("452")
                || m.contains("timeout")
                || m.contains("timed out")
                || m.contains("connection")
                || m.contains("temporarily")
                || m.contains("try again")
                || m.contains("service not available");
    }
}
