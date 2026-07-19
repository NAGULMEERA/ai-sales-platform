package com.aisales.common.contracts.catalog;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Batch offer resolution for quote/pricing paths (tenant-scoped on the server). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogOfferLookupRequest {

    @NotEmpty
    @Size(max = 100)
    @Builder.Default
    private List<UUID> offerIds = new ArrayList<>();
}
