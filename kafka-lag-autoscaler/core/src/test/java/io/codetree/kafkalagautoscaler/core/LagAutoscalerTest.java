package io.codetree.kafkalagautoscaler.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class LagAutoscalerTest {

  private static final ScalingPolicy POLICY =
      new ScalingPolicy(
          10_000, // scaleUpLagThreshold
          60_000, // scaleUpWindowMs (1 min)
          3, // scaleUpStep
          20, // maxInstances
          120_000, // scaleUpCooldownMs (2 min)
          1_000, // scaleDownLagThreshold
          300_000, // scaleDownWindowMs (5 min)
          1, // scaleDownStep
          2, // minInstances
          600_000 // scaleDownCooldownMs (10 min)
          );

  /** A clock whose current time can be advanced in tests. */
  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    void advanceMs(long millis) {
      now = now.plusMillis(millis);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static LagMetric lagOf(long lag, Instant ts) {
    return new LagMetric("grp", "topic", 0, lag, ts);
  }

  private LagAutoscaler autoscaler(int instances, MutableClock clock) {
    return new LagAutoscaler(POLICY, instances, clock);
  }

  @Test
  void holdsWhenLagIsBetweenThresholds() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(5, clock);
    assertInstanceOf(
        ScalingDecision.Hold.class, a.evaluate(List.of(lagOf(5_000, clock.instant()))));
    assertEquals(5, a.currentInstances());
  }

  @Test
  void holdsWhenScaleUpWindowNotYetElapsed() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(5, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant()))); // sets window start
    clock.advanceMs(POLICY.scaleUpWindowMs() / 2);
    assertInstanceOf(
        ScalingDecision.Hold.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
  }

  @Test
  void scalesUpAfterWindowElapses() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(5, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant())));
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    ScalingDecision d = a.evaluate(List.of(lagOf(15_000, clock.instant())));
    assertInstanceOf(ScalingDecision.ScaleUp.class, d);
    assertEquals(POLICY.scaleUpStep(), ((ScalingDecision.ScaleUp) d).delta());
    assertEquals(5 + POLICY.scaleUpStep(), a.currentInstances());
  }

  @Test
  void scaleUpRespectsMaxInstances() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(19, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant())));
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    ScalingDecision d = a.evaluate(List.of(lagOf(15_000, clock.instant())));
    assertInstanceOf(ScalingDecision.ScaleUp.class, d);
    assertEquals(1, ((ScalingDecision.ScaleUp) d).delta());
    assertEquals(20, a.currentInstances());
  }

  @Test
  void holdsWhenAlreadyAtMaxInstances() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(20, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant())));
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    assertInstanceOf(
        ScalingDecision.Hold.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
  }

  @Test
  void scalesDownAfterWindowElapses() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(10, clock);
    a.evaluate(List.of(lagOf(500, clock.instant())));
    clock.advanceMs(POLICY.scaleDownWindowMs() + 1);
    ScalingDecision d = a.evaluate(List.of(lagOf(500, clock.instant())));
    assertInstanceOf(ScalingDecision.ScaleDown.class, d);
    assertEquals(POLICY.scaleDownStep(), ((ScalingDecision.ScaleDown) d).delta());
    assertEquals(9, a.currentInstances());
  }

  @Test
  void scaleDownRespectsMinInstances() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(3, clock);
    a.evaluate(List.of(lagOf(500, clock.instant())));
    clock.advanceMs(POLICY.scaleDownWindowMs() + 1);
    ScalingDecision d = a.evaluate(List.of(lagOf(500, clock.instant())));
    assertInstanceOf(ScalingDecision.ScaleDown.class, d);
    assertEquals(1, ((ScalingDecision.ScaleDown) d).delta());
    assertEquals(2, a.currentInstances());
  }

  @Test
  void holdsWhenAlreadyAtMinInstances() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(2, clock);
    a.evaluate(List.of(lagOf(500, clock.instant())));
    clock.advanceMs(POLICY.scaleDownWindowMs() + 1);
    assertInstanceOf(ScalingDecision.Hold.class, a.evaluate(List.of(lagOf(500, clock.instant()))));
  }

  @Test
  void holdsOnEmptyMetrics() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    assertInstanceOf(ScalingDecision.Hold.class, autoscaler(5, clock).evaluate(List.of()));
  }

  @Test
  void scaleUpWindowResetsWhenLagDropsBelowThreshold() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(5, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant()))); // window starts at t=0
    clock.advanceMs(30_000);
    a.evaluate(List.of(lagOf(5_000, clock.instant()))); // mid-range lag resets window
    clock.advanceMs(POLICY.scaleUpWindowMs()); // advance full window from t=30s, but window reset
    a.evaluate(List.of(lagOf(15_000, clock.instant()))); // window starts again now
    // Only half a window has passed since the reset — should still hold
    assertInstanceOf(
        ScalingDecision.Hold.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
  }

  @Test
  void cooldownPreventsImmediateScaleUpRepeat() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(5, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant())));
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    assertInstanceOf(
        ScalingDecision.ScaleUp.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
    // Immediately try again — cooldown not elapsed
    a.evaluate(List.of(lagOf(15_000, clock.instant())));
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    assertInstanceOf(
        ScalingDecision.Hold.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
  }

  @Test
  void scalesUpAgainAfterCooldownElapses() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    LagAutoscaler a = autoscaler(5, clock);
    a.evaluate(List.of(lagOf(15_000, clock.instant())));
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    assertInstanceOf(
        ScalingDecision.ScaleUp.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
    // Advance past cooldown + window
    clock.advanceMs(POLICY.scaleUpCooldownMs() + POLICY.scaleUpWindowMs() + 1);
    a.evaluate(List.of(lagOf(15_000, clock.instant()))); // restart window
    clock.advanceMs(POLICY.scaleUpWindowMs() + 1);
    assertInstanceOf(
        ScalingDecision.ScaleUp.class, a.evaluate(List.of(lagOf(15_000, clock.instant()))));
  }

  @Test
  void rejectsInitialInstancesOutOfRange() {
    MutableClock clock = new MutableClock(Instant.EPOCH);
    assertThrows(IllegalArgumentException.class, () -> new LagAutoscaler(POLICY, 1, clock));
    assertThrows(IllegalArgumentException.class, () -> new LagAutoscaler(POLICY, 21, clock));
  }
}
