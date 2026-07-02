package com.aisales.ai.domain.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedLlmResponse {

    String content;
    String model;
    Map<String, Object> metadata;
}
