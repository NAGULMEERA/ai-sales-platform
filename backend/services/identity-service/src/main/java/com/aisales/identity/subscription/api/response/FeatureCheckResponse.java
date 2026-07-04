package com.aisales.identity.subscription.api.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class FeatureCheckResponse {

    private String featureCode;
    private boolean enabled;
    private Long limitValue;
    private String plan;
}
