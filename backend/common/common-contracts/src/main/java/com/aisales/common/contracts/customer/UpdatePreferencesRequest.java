package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {

    @Size(max = 50)
    private String preferredChannel;

    @Size(max = 20)
    private String language;

    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();
}
