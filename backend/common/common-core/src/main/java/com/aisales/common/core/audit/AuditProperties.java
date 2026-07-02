package com.aisales.common.core.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.audit")
public class AuditProperties {

    /** When true, register structured logging audit recorder if no custom bean exists. */
    private boolean loggingEnabled = true;
}
