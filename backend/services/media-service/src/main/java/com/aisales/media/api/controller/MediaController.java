package com.aisales.media.api.controller;

import com.aisales.common.contracts.media.MediaDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.media.application.service.MediaApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "Media", description = "Binary object storage (LOCAL or S3)")
public class MediaController {

    private final MediaApplicationService mediaApplicationService;

    @PostMapping(value = "/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('media:write') or hasAuthority('tenant:admin') or hasAnyRole('TENANT_ADMIN','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload a media object (PDF/text/etc.)")
    public ApiResponse<MediaDto> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(mediaApplicationService.upload(file));
    }

    @GetMapping("/objects/{id}")
    @PreAuthorize("hasAuthority('media:read') or hasAuthority('tenant:admin') or hasAnyRole('TENANT_ADMIN','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get media object metadata")
    public ApiResponse<MediaDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(mediaApplicationService.get(id));
    }

    @GetMapping("/objects/{id}/content")
    @PreAuthorize("hasAuthority('media:read') or hasAuthority('tenant:admin') or hasAnyRole('TENANT_ADMIN','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Download media bytes (tenant-scoped)")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        MediaDto meta = mediaApplicationService.get(id);
        byte[] body = mediaApplicationService.downloadContent(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (meta.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(meta.getContentType());
            } catch (Exception ignored) {
                // keep octet-stream
            }
        }
        String safeName = sanitizeFilename(meta.getOriginalFilename(), id.toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentType(mediaType)
                .body(body);
    }

    static String sanitizeFilename(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\r\\n\"\\\\]", "_").trim();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return fallback;
        }
        if (name.length() > 180) {
            name = name.substring(0, 180);
        }
        return name;
    }
}
