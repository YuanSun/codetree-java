package io.codetree.kafkalagautoscaler.otel;

import io.codetree.kafkalagautoscaler.core.LagMetric;
import io.codetree.kafkalagautoscaler.core.ScalingDecision;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes lag and scaling-decision metrics to an OpenTelemetry {@link Meter}.
 *
 * <p>Metric names mirror what is emitted in production:
 *
 * <ul>
 *   <li>{@code kafka_consumer_lag_total} — gauge, current total consumer lag
 *   <li>{@code autoscaler_scale_up_events_total} — counter, cumulative scale-up events
 *   <li>{@code autoscaler_scale_down_events_total} — counter, cumulative scale-down events
 * </ul>
 */
public final class OtelMetricsPublisher {

  private final AtomicLong currentLag = new AtomicLong(0);
  private final LongCounter scaleUpCounter;
  private final LongCounter scaleDownCounter;

  @SuppressWarnings("unused")
  private final ObservableLongGauge lagGauge;

  public OtelMetricsPublisher(Meter meter) {
    lagGauge =
        meter
            .gaugeBuilder("kafka_consumer_lag_total")
            .setDescription("Total consumer group lag across all tracked partitions")
            .ofLongs()
            .buildWithCallback(m -> m.record(currentLag.get()));

    scaleUpCounter =
        meter
            .counterBuilder("autoscaler_scale_up_events_total")
            .setDescription("Cumulative number of scale-up events emitted by the autoscaler")
            .build();

    scaleDownCounter =
        meter
            .counterBuilder("autoscaler_scale_down_events_total")
            .setDescription("Cumulative number of scale-down events emitted by the autoscaler")
            .build();
  }

  public void recordLag(List<LagMetric> metrics) {
    currentLag.set(metrics.stream().mapToLong(LagMetric::lag).sum());
  }

  public void recordDecision(ScalingDecision decision) {
    switch (decision) {
      case ScalingDecision.ScaleUp ignored -> scaleUpCounter.add(1);
      case ScalingDecision.ScaleDown ignored -> scaleDownCounter.add(1);
      case ScalingDecision.Hold ignored -> {}
    }
  }
}
