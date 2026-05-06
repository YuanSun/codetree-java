package io.codetree.kafkalagautoscaler.spring;

import io.codetree.kafkalagautoscaler.core.LagAutoscaler;
import io.codetree.kafkalagautoscaler.core.LagMetric;
import io.codetree.kafkalagautoscaler.core.LagProvider;
import io.codetree.kafkalagautoscaler.core.ScalingAction;
import io.codetree.kafkalagautoscaler.core.ScalingDecision;
import io.codetree.kafkalagautoscaler.otel.OtelMetricsPublisher;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Scheduled task that polls {@link LagProvider}, evaluates policy, and triggers scaling. */
class AutoScalerPollerTask {

  private static final Logger log = LoggerFactory.getLogger(AutoScalerPollerTask.class);

  private final LagProvider lagProvider;
  private final LagAutoscaler lagAutoscaler;
  private final ScalingAction scalingAction;
  private final KafkaLagAutoscalerProperties properties;
  private final Optional<OtelMetricsPublisher> metricsPublisher;

  AutoScalerPollerTask(
      LagProvider lagProvider,
      LagAutoscaler lagAutoscaler,
      ScalingAction scalingAction,
      KafkaLagAutoscalerProperties properties,
      Optional<OtelMetricsPublisher> metricsPublisher) {
    this.lagProvider = lagProvider;
    this.lagAutoscaler = lagAutoscaler;
    this.scalingAction = scalingAction;
    this.properties = properties;
    this.metricsPublisher = metricsPublisher;
  }

  @Scheduled(fixedDelayString = "#{@kafkaLagAutoscalerProperties.pollIntervalMs}")
  void poll() {
    try {
      List<LagMetric> metrics =
          lagProvider.fetchLag(properties.getConsumerGroup(), properties.getTopic());
      metricsPublisher.ifPresent(m -> m.recordLag(metrics));
      ScalingDecision decision = lagAutoscaler.evaluate(metrics);
      metricsPublisher.ifPresent(m -> m.recordDecision(decision));
      scalingAction.apply(decision);
    } catch (Exception e) {
      log.warn(
          "Autoscaler poll failed for group={} topic={}",
          properties.getConsumerGroup(),
          properties.getTopic(),
          e);
    }
  }
}
