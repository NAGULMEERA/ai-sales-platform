package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordConsentRequest {

    @NotBlank
    @Size(max = 100)
    private String consentType;

    @Size(max = 40)
    private String consentVersion;

    /** Defaults to true when null. */
    private Boolean granted;

    @Size(max = 50)
    private String source;
}
