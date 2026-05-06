package io.codetree.kafkalagautoscaler.spring;

import io.codetree.kafkalagautoscaler.core.LagAutoscaler;
import io.codetree.kafkalagautoscaler.core.LagProvider;
import io.codetree.kafkalagautoscaler.core.ScalingAction;
import io.codetree.kafkalagautoscaler.kafka.KafkaLagProvider;
import io.codetree.kafkalagautoscaler.otel.OtelMetricsPublisher;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires a complete autoscaling loop from {@code kafka.lag.autoscaler.*} properties. Users must
 * supply exactly one {@link ScalingAction} bean; everything else is auto-configured.
 */
@AutoConfiguration
@ConditionalOnClass(LagAutoscaler.class)
@ConditionalOnBean(ScalingAction.class)
@EnableConfigurationProperties(KafkaLagAutoscalerProperties.class)
@EnableScheduling
public class KafkaLagAutoscalerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public LagAutoscaler lagAutoscaler(KafkaLagAutoscalerProperties props) {
    return new LagAutoscaler(
        props.toScalingPolicy(), props.getInitialInstances(), Clock.systemUTC());
  }

  @Bean
  @ConditionalOnMissingBean(LagProvider.class)
  public KafkaLagProvider kafkaLagProvider(KafkaLagAutoscalerProperties props) {
    Map<String, Object> config = new HashMap<>();
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
    return new KafkaLagProvider(AdminClient.create(config));
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "io.opentelemetry.api.metrics.Meter")
  public OtelMetricsPublisher otelMetricsPublisher(
      Optional<io.opentelemetry.api.metrics.Meter> meter) {
    return meter.map(OtelMetricsPublisher::new).orElse(null);
  }

  @Bean
  public AutoScalerPollerTask autoScalerPollerTask(
      LagProvider lagProvider,
      LagAutoscaler lagAutoscaler,
      ScalingAction scalingAction,
      KafkaLagAutoscalerProperties props,
      Optional<OtelMetricsPublisher> metricsPublisher) {
    return new AutoScalerPollerTask(
        lagProvider, lagAutoscaler, scalingAction, props, metricsPublisher);
  }

  @Bean
  public KafkaLagAutoscalerProperties kafkaLagAutoscalerProperties() {
    return new KafkaLagAutoscalerProperties();
  }
}
