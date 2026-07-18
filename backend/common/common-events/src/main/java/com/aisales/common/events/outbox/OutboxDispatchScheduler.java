package com.aisales.common.events.outbox;

import com.aisales.common.events.kafka.EventKafkaHeaderPropagator;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxDispatchScheduler {

    private final OutboxClaimService claimService;
    private final OutboxKafkaSender outboxKafkaSender;
    private final EventKafkaHeaderPropagator headerPropagator;
    private final PlatformMetrics platformMetrics;
    private final ExecutorService dispatcherExecutor;

    @Value("${aisales.events.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${aisales.events.outbox.batch-size:100}")
    private int batchSize;

    public OutboxDispatchScheduler(
            OutboxClaimService claimService,
            OutboxKafkaSender outboxKafkaSender,
            EventKafkaHeaderPropagator headerPropagator,
            ObjectProvider<PlatformMetrics> platformMetrics,
            @Value("${aisales.events.outbox.parallelism:4}") int parallelism) {
        this.claimService = claimService;
        this.outboxKafkaSender = outboxKafkaSender;
        this.headerPropagator = headerPropagator;
        this.platformMetrics = platformMetrics.getIfAvailable();
        int threads = Math.max(1, parallelism);
        this.dispatcherExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "outbox-dispatcher");
            t.setDaemon(true);
            return t;
        });
    }

    @Scheduled(fixedDelayString = "${aisales.events.outbox.dispatch-interval-ms:5000}")
    public void dispatchPendingEvents() {
        List<OutboxEvent> claimed = claimService.claimBatch(Math.max(1, batchSize));
        if (claimed.isEmpty()) {
            return;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>(claimed.size());
        for (OutboxEvent event : claimed) {
            futures.add(CompletableFuture.runAsync(() -> dispatchOne(event), dispatcherExecutor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private void dispatchOne(OutboxEvent outboxEvent) {
        io.micrometer.core.instrument.Timer.Sample sample =
                platformMetrics != null ? platformMetrics.startTimer() : null;
        try {
            String key = outboxEvent.getAggregateId();
            ProducerRecord<String, String> record = headerPropagator.enrichProducerRecord(
                    outboxEvent.getTopic(), key, outboxEvent.getPayload());
            outboxKafkaSender.send(record);
            outboxEvent.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            outboxEvent.setPublishedAt(Instant.now());
            claimService.save(outboxEvent);
            if (platformMetrics != null) {
                platformMetrics.increment(MetricNames.OUTBOX_DISPATCHED, "status", "published");
            }
        } catch (Exception ex) {
            outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
            if (outboxEvent.getRetryCount() >= maxRetries) {
                outboxEvent.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.error("Outbox event {} permanently failed after {} retries",
                        outboxEvent.getId(), outboxEvent.getRetryCount(), ex);
                if (platformMetrics != null) {
                    platformMetrics.increment(MetricNames.OUTBOX_DISPATCHED, "status", "failed");
                }
            } else {
                outboxEvent.setStatus(OutboxEvent.OutboxStatus.PENDING);
                log.warn("Outbox event {} dispatch failed (retry {}): {}",
                        outboxEvent.getId(), outboxEvent.getRetryCount(), ex.getMessage());
                if (platformMetrics != null) {
                    platformMetrics.increment(MetricNames.OUTBOX_DISPATCHED, "status", "retry");
                }
            }
            claimService.save(outboxEvent);
        } finally {
            if (sample != null && platformMetrics != null) {
                platformMetrics.recordTimer(sample, MetricNames.KAFKA_PUBLISH_LATENCY,
                        "publisher", "outbox");
            }
        }
    }

    @PreDestroy
    void shutdown() {
        dispatcherExecutor.shutdown();
        try {
            if (!dispatcherExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                dispatcherExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            dispatcherExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
