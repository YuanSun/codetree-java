package io.codetree.kafkalagautoscaler.core;

/** Consumer-side integration point: receives every scaling decision from {@link LagAutoscaler}. */
@FunctionalInterface
public interface ScalingAction {

  void apply(ScalingDecision decision);
}
