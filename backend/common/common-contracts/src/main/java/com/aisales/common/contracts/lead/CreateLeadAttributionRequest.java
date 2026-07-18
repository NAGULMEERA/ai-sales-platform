package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeadAttributionRequest {

    @NotBlank
    @Size(max = 50)
    private String channel;

    @Size(max = 255)
    private String campaign;

    @Size(max = 255)
    private String adId;

    private Integer position;

    private BigDecimal cost;

    private Map<String, Object> sourceDetails;
}
