package com.aisales.media.api.controller;

import com.aisales.common.contracts.media.MediaDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.media.application.service.MediaApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Media", description = "Binary object storage (LOCAL or S3)")
public class MediaController {

    private final MediaApplicationService mediaApplicationService;

    @PostMapping(value = "/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a media object (PDF/text/etc.)")
    public ApiResponse<MediaDto> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(mediaApplicationService.upload(file));
    }

    @GetMapping("/objects/{id}")
    @Operation(summary = "Get media object metadata")
    public ApiResponse<MediaDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(mediaApplicationService.get(id));
    }

    @GetMapping("/objects/{id}/content")
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
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                        + (meta.getOriginalFilename() != null ? meta.getOriginalFilename() : id) + "\"")
                .contentType(mediaType)
                .body(body);
    }
}
