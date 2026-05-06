package io.codetree.kafkalagautoscaler.kafka;

import io.codetree.kafkalagautoscaler.core.LagFetchException;
import io.codetree.kafkalagautoscaler.core.LagMetric;
import io.codetree.kafkalagautoscaler.core.LagProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;

/**
 * {@link LagProvider} backed by {@link AdminClient}. Fetches committed offsets and end offsets for
 * every partition of the given topic, then returns per-partition lag metrics.
 */
public final class KafkaLagProvider implements LagProvider, AutoCloseable {

  private final AdminClient adminClient;

  public KafkaLagProvider(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public List<LagMetric> fetchLag(String consumerGroup, String topic) {
    try {
      Map<TopicPartition, Long> committedOffsets = fetchCommittedOffsets(consumerGroup, topic);
      if (committedOffsets.isEmpty()) {
        return List.of();
      }
      Map<TopicPartition, Long> endOffsets =
          fetchEndOffsets(committedOffsets.keySet().stream().toList());
      Instant now = Instant.now();
      return committedOffsets.entrySet().stream()
          .map(
              e -> {
                TopicPartition tp = e.getKey();
                long committed = e.getValue();
                long end = endOffsets.getOrDefault(tp, committed);
                long lag = Math.max(0L, end - committed);
                return new LagMetric(consumerGroup, tp.topic(), tp.partition(), lag, now);
              })
          .toList();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LagFetchException("Interrupted while fetching lag for group " + consumerGroup, e);
    } catch (ExecutionException e) {
      throw new LagFetchException("Failed to fetch lag for group " + consumerGroup, e.getCause());
    }
  }

  private Map<TopicPartition, Long> fetchCommittedOffsets(String consumerGroup, String topic)
      throws ExecutionException, InterruptedException {
    return adminClient
        .listConsumerGroupOffsets(consumerGroup)
        .partitionsToOffsetAndMetadata()
        .get()
        .entrySet()
        .stream()
        .filter(e -> e.getKey().topic().equals(topic))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
  }

  private Map<TopicPartition, Long> fetchEndOffsets(List<TopicPartition> partitions)
      throws ExecutionException, InterruptedException {
    Map<TopicPartition, OffsetSpec> request =
        partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
    return adminClient.listOffsets(request).all().get().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
  }

  @Override
  public void close() {
    adminClient.close();
  }
}
