package io.codetree.kafkalagautoscaler.core;

import java.util.List;

/** Fetches a current lag snapshot for a consumer group / topic pair. */
public interface LagProvider {

  List<LagMetric> fetchLag(String consumerGroup, String topic);
}
