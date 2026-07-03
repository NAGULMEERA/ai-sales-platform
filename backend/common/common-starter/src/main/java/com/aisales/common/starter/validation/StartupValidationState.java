package com.aisales.common.starter.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartupValidationState {

    private volatile boolean validated;
    private volatile List<String> failures = List.of();

    public void markValidated() {
        this.validated = true;
        this.failures = List.of();
    }

    public void markFailed(List<String> failures) {
        this.validated = false;
        this.failures = List.copyOf(failures);
    }

    public boolean isValidated() {
        return validated;
    }

    public List<String> getFailures() {
        return new ArrayList<>(failures);
    }
}
