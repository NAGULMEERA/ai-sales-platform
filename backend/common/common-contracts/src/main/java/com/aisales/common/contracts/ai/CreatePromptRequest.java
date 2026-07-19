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
public class CreatePromptRequest {

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String purpose;

    @Size(max = 64)
    private String industryCode;

    @Size(max = 16)
    private String languageCode;

    @Size(max = 64)
    private String capability;

    @Size(max = 128)
    private String preferredModel;

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
}
