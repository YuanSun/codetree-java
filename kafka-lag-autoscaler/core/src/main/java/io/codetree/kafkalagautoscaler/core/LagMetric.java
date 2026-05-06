package io.codetree.kafkalagautoscaler.core;

import java.time.Instant;

/** A point-in-time lag observation for a single partition. */
public record LagMetric(
    String consumerGroup, String topic, int partition, long lag, Instant timestamp) {}
