package io.codetree.kafkalagautoscaler.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Stateful evaluator that turns a stream of {@link LagMetric} snapshots into {@link
 * ScalingDecision}s according to a {@link ScalingPolicy}.
 *
 * <p>Not thread-safe — callers must serialize access (e.g. a single-threaded scheduler).
 */
public final class LagAutoscaler {

  private final ScalingPolicy policy;
  private final Clock clock;

  private int currentInstances;
  private Instant scaleUpWindowStart;
  private Instant scaleDownWindowStart;
  private Instant lastScaleUpAt;
  private Instant lastScaleDownAt;

  public LagAutoscaler(ScalingPolicy policy, int initialInstances, Clock clock) {
    if (initialInstances < policy.minInstances() || initialInstances > policy.maxInstances()) {
      throw new IllegalArgumentException(
          "initialInstances (%d) must be between minInstances (%d) and maxInstances (%d)"
              .formatted(initialInstances, policy.minInstances(), policy.maxInstances()));
    }
    this.policy = policy;
    this.currentInstances = initialInstances;
    this.clock = clock;
  }

  /**
   * Evaluates the current lag snapshot and returns a scaling decision. An empty list is treated as
   * a no-data cycle and always returns {@link ScalingDecision.Hold}.
   */
  public ScalingDecision evaluate(List<LagMetric> metrics) {
    if (metrics.isEmpty()) {
      return new ScalingDecision.Hold();
    }

    long totalLag = metrics.stream().mapToLong(LagMetric::lag).sum();
    Instant now = clock.instant();

    if (totalLag >= policy.scaleUpLagThreshold()) {
      return handleHighLag(now);
    } else if (totalLag <= policy.scaleDownLagThreshold()) {
      return handleLowLag(now);
    } else {
      scaleUpWindowStart = null;
      scaleDownWindowStart = null;
      return new ScalingDecision.Hold();
    }
  }

  private ScalingDecision handleHighLag(Instant now) {
    scaleDownWindowStart = null;
    if (scaleUpWindowStart == null) {
      scaleUpWindowStart = now;
    }
    if (windowElapsed(scaleUpWindowStart, now, policy.scaleUpWindowMs())
        && cooldownElapsed(lastScaleUpAt, now, policy.scaleUpCooldownMs())
        && currentInstances < policy.maxInstances()) {
      int delta = Math.min(policy.scaleUpStep(), policy.maxInstances() - currentInstances);
      currentInstances += delta;
      lastScaleUpAt = now;
      scaleUpWindowStart = null;
      return new ScalingDecision.ScaleUp(delta);
    }
    return new ScalingDecision.Hold();
  }

  private ScalingDecision handleLowLag(Instant now) {
    scaleUpWindowStart = null;
    if (scaleDownWindowStart == null) {
      scaleDownWindowStart = now;
    }
    if (windowElapsed(scaleDownWindowStart, now, policy.scaleDownWindowMs())
        && cooldownElapsed(lastScaleDownAt, now, policy.scaleDownCooldownMs())
        && currentInstances > policy.minInstances()) {
      int delta = Math.min(policy.scaleDownStep(), currentInstances - policy.minInstances());
      currentInstances -= delta;
      lastScaleDownAt = now;
      scaleDownWindowStart = null;
      return new ScalingDecision.ScaleDown(delta);
    }
    return new ScalingDecision.Hold();
  }

  private static boolean windowElapsed(Instant start, Instant now, long windowMs) {
    return Duration.between(start, now).toMillis() >= windowMs;
  }

  private static boolean cooldownElapsed(Instant lastAction, Instant now, long cooldownMs) {
    return lastAction == null || Duration.between(lastAction, now).toMillis() >= cooldownMs;
  }

  public int currentInstances() {
    return currentInstances;
  }
}
