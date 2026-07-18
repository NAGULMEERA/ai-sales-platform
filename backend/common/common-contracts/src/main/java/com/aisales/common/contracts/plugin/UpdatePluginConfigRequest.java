package com.aisales.common.contracts.plugin;

import jakarta.validation.constraints.NotNull;
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
public class UpdatePluginConfigRequest {

    @NotNull
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();
}
