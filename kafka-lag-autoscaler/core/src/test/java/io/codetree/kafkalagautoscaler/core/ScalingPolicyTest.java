package io.codetree.kafkalagautoscaler.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ScalingPolicyTest {

  private static ScalingPolicy valid() {
    return new ScalingPolicy(10_000, 120_000, 5, 40, 180_000, 1_000, 600_000, 2, 8, 600_000);
  }

  @Test
  void validPolicyIsCreated() {
    assertDoesNotThrow(ScalingPolicyTest::valid);
  }

  @Test
  void rejectsScaleUpThresholdBelowScaleDown() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScalingPolicy(500, 120_000, 5, 40, 180_000, 1_000, 600_000, 2, 8, 600_000));
  }

  @Test
  void rejectsMinInstancesGreaterThanMax() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScalingPolicy(10_000, 120_000, 5, 5, 180_000, 1_000, 600_000, 2, 8, 600_000));
  }

  @Test
  void rejectsNegativeScaleUpStep() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScalingPolicy(10_000, 120_000, -1, 40, 180_000, 1_000, 600_000, 2, 8, 600_000));
  }

  @Test
  void rejectsZeroScaleUpWindow() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScalingPolicy(10_000, 0, 5, 40, 180_000, 1_000, 600_000, 2, 8, 600_000));
  }
}
