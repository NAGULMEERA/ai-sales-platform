package com.aisales.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable duplicate / identity matching strategy.
 */
@Data
@ConfigurationProperties(prefix = "aisales.customer.matching")
public class CustomerMatchingProperties {

    private boolean matchPhone = true;
    private boolean matchEmail = true;
    private boolean matchWhatsapp = true;
    private boolean matchExternalCrmId = true;
    /** Off by default — enable per tenant/compliance needs. */
    private boolean matchGovernmentId = false;
    /** Number of weak signals required for PROBABLE when no exact signal hits. */
    private int weakMatchThreshold = 2;
}
