package com.aisales.common.contracts.plugin;

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
public class EnablePluginRequest {

    /** Tenant-specific plugin configuration (validated against catalog config schema later). */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();
}
