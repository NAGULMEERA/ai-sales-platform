package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes document extraction by {@code aisales.ai.rag.extractor}
 * ({@code AUTO} | {@code TEXT} | {@code PDF}).
 */
@Component
@RequiredArgsConstructor
public class DocumentExtractorRegistry {

    private final List<DocumentExtractor> extractors;
    private final RagProperties properties;

    public DocumentExtractor resolve(String override, String contentType, String filename) {
        String configured = StringUtils.hasText(override) ? override : properties.getExtractor();
        if (!StringUtils.hasText(configured)) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "aisales.ai.rag.extractor is not set");
        }
        String key = configured.trim().toUpperCase(Locale.ROOT);
        if ("AUTO".equals(key)) {
            return extractors.stream()
                    .filter(e -> e.supports(contentType, filename))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException(
                            "No document extractor supports contentType="
                                    + contentType
                                    + " filename="
                                    + filename
                                    + ". Set aisales.ai.rag.extractor=TEXT|PDF or provide a supported file."));
        }
        return extractors.stream()
                .filter(e -> key.equals(e.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AI_UNAVAILABLE,
                        "No document extractor registered for aisales.ai.rag.extractor="
                                + key
                                + ". Available: AUTO, "
                                + extractors.stream()
                                        .map(DocumentExtractor::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "))));
    }
}
