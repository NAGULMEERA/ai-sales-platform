package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreatePromptVersionRequest {

    @Size(max = 20000)
    private String systemTemplate;

    @NotBlank
    @Size(max = 20000)
    private String userTemplate;

    @Builder.Default
    private List<String> variables = new ArrayList<>();

    @Size(max = 2000)
    private String expectedOutputHint;

    @Size(max = 2000)
    private String changelog;

    /** When true, activates this version immediately. */
    private Boolean activate;
}
