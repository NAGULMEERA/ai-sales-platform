package com.aisales.media.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaControllerFilenameTest {

    @Test
    void shouldStripPathAndHeaderInjectionCharacters() {
        assertThat(MediaController.sanitizeFilename("../../evil\r\nX: 1\".pdf", "fallback"))
                .isEqualTo("evil__X: 1_.pdf");
        assertThat(MediaController.sanitizeFilename(null, "id-1")).isEqualTo("id-1");
        assertThat(MediaController.sanitizeFilename("  ", "id-1")).isEqualTo("id-1");
    }
}
