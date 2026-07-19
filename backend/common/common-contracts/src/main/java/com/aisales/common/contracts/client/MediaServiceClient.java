package com.aisales.common.contracts.client;

import com.aisales.common.contracts.media.MediaDto;
import com.aisales.common.core.dto.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "media-service",
        path = "/api/v1/media",
        url = "${aisales.clients.media-service.url:}")
public interface MediaServiceClient {

    @GetMapping("/objects/{id}")
    ApiResponse<MediaDto> getObject(@PathVariable UUID id);

    @GetMapping(value = "/objects/{id}/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] downloadContent(@PathVariable UUID id);
}
