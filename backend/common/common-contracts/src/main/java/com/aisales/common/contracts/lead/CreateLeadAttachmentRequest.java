package com.aisales.common.contracts.lead;

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
public class CreateLeadAttachmentRequest {

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    private String fileUrl;

    @Size(max = 100)
    private String fileType;

    private Long fileSize;
}
