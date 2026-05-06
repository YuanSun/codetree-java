package io.codetree.kafkalagautoscaler.core;

/** The outcome of a single autoscaler evaluation cycle. */
public sealed interface ScalingDecision {

  record ScaleUp(int delta) implements ScalingDecision {}

  record ScaleDown(int delta) implements ScalingDecision {}

  record Hold() implements ScalingDecision {}
}
