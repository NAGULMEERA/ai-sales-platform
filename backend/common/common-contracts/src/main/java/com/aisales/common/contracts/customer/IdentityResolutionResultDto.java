package com.aisales.common.contracts.customer;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityResolutionResultDto {

    private IdentityMatchType matchType;

    @Builder.Default
    private List<IdentityMatchCandidateDto> candidates = new ArrayList<>();
}
