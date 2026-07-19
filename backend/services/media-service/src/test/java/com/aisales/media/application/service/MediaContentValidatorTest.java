package com.aisales.media.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.ValidationException;
import com.aisales.media.infrastructure.configuration.MediaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MediaContentValidatorTest {

    private MediaContentValidator validator;

    @BeforeEach
    void setUp() {
        MediaProperties properties = new MediaProperties();
        validator = new MediaContentValidator(properties);
    }

    @Test
    void shouldAcceptJpegWithMatchingMagicBytes() {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg);

        assertThatCode(() -> validator.validate(file, jpeg)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBlockedExtension() {
        byte[] payload = "%PDF-1.4".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/pdf", payload);

        assertThatThrownBy(() -> validator.validate(file, payload))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("extension");
    }

    @Test
    void shouldRejectContentTypeMismatch() {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "application/pdf", jpeg);

        assertThatThrownBy(() -> validator.validate(file, jpeg))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void shouldRejectDisallowedContentType() {
        byte[] payload = "hello".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "x.bin", "application/x-msdownload", payload);

        assertThatThrownBy(() -> validator.validate(file, payload))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Content type");
    }
}
