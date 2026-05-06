package io.codetree.kafkalagautoscaler.core;

/**
 * Immutable value object holding every threshold that governs autoscaling behaviour. Construction
 * fails fast with {@link IllegalArgumentException} if any parameter is logically invalid.
 */
public record ScalingPolicy(
    long scaleUpLagThreshold,
    long scaleUpWindowMs,
    int scaleUpStep,
    int maxInstances,
    long scaleUpCooldownMs,
    long scaleDownLagThreshold,
    long scaleDownWindowMs,
    int scaleDownStep,
    int minInstances,
    long scaleDownCooldownMs) {

  public ScalingPolicy {
    require(scaleUpLagThreshold > 0, "scaleUpLagThreshold must be positive");
    require(scaleDownLagThreshold >= 0, "scaleDownLagThreshold must be non-negative");
    require(
        scaleUpLagThreshold > scaleDownLagThreshold,
        "scaleUpLagThreshold must be greater than scaleDownLagThreshold");
    require(scaleUpWindowMs > 0, "scaleUpWindowMs must be positive");
    require(scaleDownWindowMs > 0, "scaleDownWindowMs must be positive");
    require(scaleUpStep > 0, "scaleUpStep must be positive");
    require(scaleDownStep > 0, "scaleDownStep must be positive");
    require(minInstances >= 0, "minInstances must be non-negative");
    require(maxInstances > 0, "maxInstances must be positive");
    require(maxInstances > minInstances, "maxInstances must be greater than minInstances");
    require(scaleUpCooldownMs >= 0, "scaleUpCooldownMs must be non-negative");
    require(scaleDownCooldownMs >= 0, "scaleDownCooldownMs must be non-negative");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
