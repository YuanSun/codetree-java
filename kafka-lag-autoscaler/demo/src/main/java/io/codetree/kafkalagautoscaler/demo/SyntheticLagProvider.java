package io.codetree.kafkalagautoscaler.demo;

import io.codetree.kafkalagautoscaler.core.LagMetric;
import io.codetree.kafkalagautoscaler.core.LagProvider;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Generates a repeating synthetic lag pattern so the demo works without a real Kafka cluster.
 *
 * <p>Cycle (100 ticks): ramp up 0→50k (ticks 0-29), sustain 50k (ticks 30-59), ramp down 50k→300
 * (ticks 60-89), flat 300 (ticks 90-99).
 */
@Component
public class SyntheticLagProvider implements LagProvider {

  private final AtomicInteger tick = new AtomicInteger(0);

  @Override
  public List<LagMetric> fetchLag(String consumerGroup, String topic) {
    int t = tick.getAndIncrement() % 100;
    long lag;
    if (t < 30) {
      lag = t * 1_700L; // 0 → 49 800
    } else if (t < 60) {
      lag = 50_000L;
    } else if (t < 90) {
      lag = Math.max(300L, 50_000L - (t - 60) * 1_700L);
    } else {
      lag = 300L;
    }
    return List.of(new LagMetric(consumerGroup, topic, 0, lag, Instant.now()));
  }
}
