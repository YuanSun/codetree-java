package io.codetree.kafkalagautoscaler.spring;

import io.codetree.kafkalagautoscaler.core.ScalingPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code kafka.lag.autoscaler.*} properties and constructs a {@link ScalingPolicy}. */
@ConfigurationProperties(prefix = "kafka.lag.autoscaler")
public class KafkaLagAutoscalerProperties {

  /** Kafka consumer group to monitor. */
  private String consumerGroup;

  /** Topic to monitor. */
  private String topic;

  /** Bootstrap servers used to create the AdminClient when no {@code LagProvider} bean exists. */
  private String bootstrapServers = "localhost:9092";

  /** Current replica count at startup — must be within [minInstances, maxInstances]. */
  private int initialInstances;

  /** How often to poll Kafka for lag. Defaults to 10 s. */
  private long pollIntervalMs = 10_000;

  private long scaleUpLagThreshold;
  private long scaleUpWindowMs;
  private int scaleUpStep;
  private int maxInstances;
  private long scaleUpCooldownMs;
  private long scaleDownLagThreshold;
  private long scaleDownWindowMs;
  private int scaleDownStep;
  private int minInstances;
  private long scaleDownCooldownMs;

  public ScalingPolicy toScalingPolicy() {
    return new ScalingPolicy(
        scaleUpLagThreshold,
        scaleUpWindowMs,
        scaleUpStep,
        maxInstances,
        scaleUpCooldownMs,
        scaleDownLagThreshold,
        scaleDownWindowMs,
        scaleDownStep,
        minInstances,
        scaleDownCooldownMs);
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public int getInitialInstances() {
    return initialInstances;
  }

  public void setInitialInstances(int initialInstances) {
    this.initialInstances = initialInstances;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public long getScaleUpLagThreshold() {
    return scaleUpLagThreshold;
  }

  public void setScaleUpLagThreshold(long scaleUpLagThreshold) {
    this.scaleUpLagThreshold = scaleUpLagThreshold;
  }

  public long getScaleUpWindowMs() {
    return scaleUpWindowMs;
  }

  public void setScaleUpWindowMs(long scaleUpWindowMs) {
    this.scaleUpWindowMs = scaleUpWindowMs;
  }

  public int getScaleUpStep() {
    return scaleUpStep;
  }

  public void setScaleUpStep(int scaleUpStep) {
    this.scaleUpStep = scaleUpStep;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public long getScaleUpCooldownMs() {
    return scaleUpCooldownMs;
  }

  public void setScaleUpCooldownMs(long scaleUpCooldownMs) {
    this.scaleUpCooldownMs = scaleUpCooldownMs;
  }

  public long getScaleDownLagThreshold() {
    return scaleDownLagThreshold;
  }

  public void setScaleDownLagThreshold(long scaleDownLagThreshold) {
    this.scaleDownLagThreshold = scaleDownLagThreshold;
  }

  public long getScaleDownWindowMs() {
    return scaleDownWindowMs;
  }

  public void setScaleDownWindowMs(long scaleDownWindowMs) {
    this.scaleDownWindowMs = scaleDownWindowMs;
  }

  public int getScaleDownStep() {
    return scaleDownStep;
  }

  public void setScaleDownStep(int scaleDownStep) {
    this.scaleDownStep = scaleDownStep;
  }

  public int getMinInstances() {
    return minInstances;
  }

  public void setMinInstances(int minInstances) {
    this.minInstances = minInstances;
  }

  public long getScaleDownCooldownMs() {
    return scaleDownCooldownMs;
  }

  public void setScaleDownCooldownMs(long scaleDownCooldownMs) {
    this.scaleDownCooldownMs = scaleDownCooldownMs;
  }
}
