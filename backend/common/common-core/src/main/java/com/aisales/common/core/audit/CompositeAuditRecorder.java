package com.aisales.common.core.audit;

import lombok.RequiredArgsConstructor;

import java.util.List;

/** Fan-out audit records to multiple sinks (e.g. JDBC + logging + Kafka). */
@RequiredArgsConstructor
public class CompositeAuditRecorder implements AuditRecorder {

    private final List<AuditRecorder> delegates;

    @Override
    public void record(AuditRecord record) {
        for (AuditRecorder delegate : delegates) {
            delegate.record(record);
        }
    }
}
