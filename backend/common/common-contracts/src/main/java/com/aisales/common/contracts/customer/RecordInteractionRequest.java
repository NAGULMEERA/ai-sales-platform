package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.NotBlank;
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
public class RecordInteractionRequest {

    @NotBlank
    @Size(max = 80)
    private String interactionType;

    @NotBlank
    @Size(max = 50)
    private String channel;

    @Size(max = 1000)
    private String summary;

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();
}
