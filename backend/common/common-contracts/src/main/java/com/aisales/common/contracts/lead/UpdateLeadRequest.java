package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeadRequest {

    @Size(max = 255)
    private String customerName;

    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String sourceType;

    @Size(max = 255)
    private String sourceId;

    @Size(max = 255)
    private String campaign;

    /** When non-null, replaces industry-agnostic lead attributes. */
    private Map<String, Object> attributes;
}
