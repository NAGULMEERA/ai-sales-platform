package com.aisales.common.observability.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class TransientFailurePredicateTest {

    private final TransientFailurePredicate predicate = new TransientFailurePredicate();

    @Test
    void shouldTreatConnectTimeoutAsTransient() {
        assertThat(predicate.test(new ResourceAccessException("timeout", new SocketTimeoutException()))).isTrue();
    }

    @Test
    void shouldTreatConnectionRefusedAsTransient() {
        assertThat(predicate.test(new ResourceAccessException("refused", new ConnectException()))).isTrue();
    }

    @Test
    void shouldTreatServerErrorAsTransient() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null);
        assertThat(predicate.test(ex)).isTrue();
    }

    @Test
    void shouldTreatTooManyRequestsAsTransient() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
        assertThat(predicate.test(ex)).isTrue();
    }

    @Test
    void shouldNotRetryBadRequest() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);
        assertThat(predicate.test(ex)).isFalse();
    }

    @Test
    void shouldNotRetryUnauthorized() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        assertThat(predicate.test(ex)).isFalse();
    }

    @Test
    void shouldNotRetryUnrecognizedExceptionTypes() {
        assertThat(predicate.test(new IllegalStateException("business rule violated"))).isFalse();
    }

    @Test
    void shouldFindTransientCauseThroughWrapperExceptions() {
        RuntimeException wrapped = new RuntimeException("wrapper", new SocketTimeoutException("read timed out"));
        assertThat(predicate.test(wrapped)).isTrue();
    }

    @Test
    void shouldHandleNullSafely() {
        assertThat(predicate.test(null)).isFalse();
    }

    @Test
    void shouldTreatSmtp421AsTransient() {
        assertThat(predicate.test(new FakeMailSendException("421 Service not available"))).isTrue();
    }

    @Test
    void shouldNotRetrySmtpAuthenticationFailure() {
        assertThat(predicate.test(new FakeMailAuthenticationException("535 Authentication failed")))
                .isFalse();
    }

    /** Class name must end with MailSendException for predicate matching. */
    @SuppressWarnings("serial")
    static final class FakeMailSendException extends RuntimeException {
        FakeMailSendException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    static final class FakeMailAuthenticationException extends RuntimeException {
        FakeMailAuthenticationException(String message) {
            super(message);
        }
    }
}
