package com.aisales.media.application.service;

import com.aisales.common.exception.exception.ValidationException;
import com.aisales.media.infrastructure.configuration.MediaProperties;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads against allowlisted content types, blocked extensions, and magic bytes.
 */
@Component
@RequiredArgsConstructor
public class MediaContentValidator {

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "dll", "bat", "cmd", "com", "msi", "scr", "ps1", "sh", "bash",
            "js", "jar", "war", "ear", "php", "jsp", "asp", "aspx", "cgi", "py", "rb");

    private final MediaProperties mediaProperties;

    public void validate(MultipartFile file, byte[] bytes) {
        if (file == null || bytes == null) {
            throw new ValidationException("file is required");
        }
        long maxBytes = mediaProperties.getMaxBytes();
        if (bytes.length > maxBytes) {
            throw new ValidationException("File exceeds maximum allowed size of " + maxBytes + " bytes");
        }

        String filename = file.getOriginalFilename();
        String extension = extensionOf(filename);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("File extension is not allowed: " + extension);
        }

        String declared = normalizeContentType(file.getContentType());
        if (!mediaProperties.getAllowedContentTypes().contains(declared)) {
            throw new ValidationException("Content type is not allowed: " + declared);
        }

        if (mediaProperties.isValidateMagicBytes() && !magicMatches(declared, bytes)) {
            throw new ValidationException("File content does not match declared content type");
        }
    }

    static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        String value = contentType.trim().toLowerCase(Locale.ROOT);
        int semi = value.indexOf(';');
        return semi >= 0 ? value.substring(0, semi).trim() : value;
    }

    static String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    static boolean magicMatches(String contentType, byte[] bytes) {
        if (bytes.length == 0) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> startsWith(bytes, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
            case "image/png" -> startsWith(bytes, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
            case "image/gif" -> startsWith(bytes, "GIF87a") || startsWith(bytes, "GIF89a");
            case "image/webp" -> startsWith(bytes, "RIFF") && bytes.length >= 12 && startsWithAt(bytes, 8, "WEBP");
            case "application/pdf" -> startsWith(bytes, "%PDF");
            case "application/zip",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    startsWith(bytes, "PK");
            case "text/plain", "text/csv", "text/markdown" -> looksLikeText(bytes);
            case "application/json" -> looksLikeText(bytes) && (bytes[0] == '{' || bytes[0] == '[');
            default -> true; // allowlisted type without a strict signature
        };
    }

    private static boolean startsWith(byte[] bytes, String ascii) {
        byte[] prefix = ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return startsWith(bytes, prefix);
    }

    private static boolean startsWith(byte[] bytes, byte... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithAt(byte[] bytes, int offset, String ascii) {
        byte[] prefix = ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (bytes.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeText(byte[] bytes) {
        int limit = Math.min(bytes.length, 512);
        for (int i = 0; i < limit; i++) {
            byte b = bytes[i];
            if (b == 0) {
                return false;
            }
            // Allow common UTF-8 / ASCII text bytes; reject obvious binary control chars.
            if (b < 0x09 || (b > 0x0D && b < 0x20 && b != 0x1B)) {
                return false;
            }
        }
        return true;
    }
}
