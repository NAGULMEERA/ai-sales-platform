package com.aisales.common.contracts.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Lightweight contract checks for the additive catalog offer lookup request
 * (keeps Feign/API consumers aligned without a separate OpenAPI runner).
 */
class CatalogOfferLookupRequestContractTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidOfferIdList() {
        CatalogOfferLookupRequest request = CatalogOfferLookupRequest.builder()
                .offerIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectEmptyOfferIds() {
        CatalogOfferLookupRequest request =
                CatalogOfferLookupRequest.builder().offerIds(List.of()).build();

        Set<ConstraintViolation<CatalogOfferLookupRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectMoreThanHundredOfferIds() {
        List<UUID> ids = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();
        CatalogOfferLookupRequest request =
                CatalogOfferLookupRequest.builder().offerIds(ids).build();

        Set<ConstraintViolation<CatalogOfferLookupRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("offerIds"));
    }
}
