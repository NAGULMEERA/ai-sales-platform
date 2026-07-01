package com.company.platform.template.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Marker interface for Value Objects.
 */
public interface ValueObject {}

/**
 * Production-ready Email Value Object.
 */
public record Email(String value) implements ValueObject {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        value = value.toLowerCase().trim();
    }
}

/**
 * Money Value Object.
 */
public record Money(BigDecimal amount, String currency) implements ValueObject {

    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        currency = currency.toUpperCase();
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }
}

/**
 * Phone Number Value Object.
 */
public record PhoneNumber(String value) implements ValueObject {

    public PhoneNumber {
        Objects.requireNonNull(value);

        String normalized = value.replaceAll("\\s+", "");

        if (!normalized.matches("^\\+?[0-9]{10,15}$")) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        value = normalized;
    }
}
