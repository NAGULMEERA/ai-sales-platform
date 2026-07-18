package com.aisales.common.events.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImmutableEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void baseEventAndUserCreatedShouldNotExposePublicSetters() {
        assertNoPublicSetters(BaseEvent.class);
        assertNoPublicSetters(UserCreatedEvent.class);
        assertNoPublicSetters(LeadCreatedEvent.class);
    }

    @Test
    void eventsShouldCarryEventVersionAndRoundTripWithoutSetters() throws Exception {
        UserCreatedEvent original = UserCreatedEvent.of(
                "tenant-1", "user-1", "a@b.com", "Ada", "Lovelace", Set.of("ADMIN"), "corr-1");
        assertThat(original.getEventVersion()).isEqualTo(1);

        String json = objectMapper.writeValueAsString(original);
        UserCreatedEvent restored = objectMapper.readValue(json, UserCreatedEvent.class);

        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getEventVersion()).isEqualTo(1);
        assertThat(restored.getEmail()).isEqualTo("a@b.com");
        assertThat(restored.getTenantId()).isEqualTo("tenant-1");
    }

    private static void assertNoPublicSetters(Class<?> type) {
        Method[] setters = Arrays.stream(type.getMethods())
                .filter(m -> m.getDeclaringClass() == type)
                .filter(m -> m.getName().startsWith("set") && m.getParameterCount() == 1)
                .toArray(Method[]::new);
        assertThat(setters).as("%s must not expose public setters", type.getSimpleName()).isEmpty();
    }
}
