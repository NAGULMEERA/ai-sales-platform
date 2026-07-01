package com.aisales.common.contracts.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {

    private UUID id;
    private String name;
    private String slug;
    private String plan;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
