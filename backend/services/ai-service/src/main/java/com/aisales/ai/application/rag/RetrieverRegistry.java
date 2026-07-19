package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RetrieverRegistry {

    private final Map<String, Retriever> byName;
    private final RagProperties ragProperties;

    public RetrieverRegistry(List<Retriever> retrievers, RagProperties ragProperties) {
        this.byName = retrievers.stream()
                .collect(Collectors.toMap(
                        r -> r.name().trim().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (a, b) -> a));
        this.ragProperties = ragProperties;
    }

    public Retriever resolveDefault() {
        String configured = ragProperties.getRetriever();
        String key = StringUtils.hasText(configured)
                ? configured.trim().toUpperCase(Locale.ROOT)
                : "VECTOR";
        Retriever retriever = byName.get(key);
        if (retriever == null) {
            retriever = byName.get("VECTOR");
        }
        if (retriever == null) {
            throw new ValidationException("No Retriever registered");
        }
        return retriever;
    }

    public Retriever resolve(String name) {
        if (!StringUtils.hasText(name)) {
            return resolveDefault();
        }
        Retriever retriever = byName.get(name.trim().toUpperCase(Locale.ROOT));
        if (retriever == null) {
            throw new ValidationException("Unknown retriever: " + name);
        }
        return retriever;
    }
}
