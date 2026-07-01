package com.aisales.identity.application.utils;

public final class SlugGenerator {

    private SlugGenerator() {
    }

    public static String generate(String companyName) {
        return companyName
            .trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-");
    }

    public static String generateUnique(String companyName) {
        return generate(companyName) + "-" + System.currentTimeMillis();
    }
}
