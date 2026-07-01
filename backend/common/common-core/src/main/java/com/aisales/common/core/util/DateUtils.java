package com.aisales.common.core.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private DateUtils() {
    }

    public static Instant now() {
        return Instant.now();
    }

    public static String formatInstant(Instant instant) {
        return ISO_FORMATTER.format(instant);
    }

    public static LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
