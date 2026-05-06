package io.codetree.kafkalagautoscaler.demo;

import io.codetree.kafkalagautoscaler.core.ScalingAction;
import io.codetree.kafkalagautoscaler.core.ScalingDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs every scaling decision — the simplest possible {@link ScalingAction} implementation. */
@Component
public class LoggingScalingAction implements ScalingAction {

  private static final Logger log = LoggerFactory.getLogger(LoggingScalingAction.class);

  @Override
  public void apply(ScalingDecision decision) {
    switch (decision) {
      case ScalingDecision.ScaleUp up -> log.info("SCALE UP  +{} instances", up.delta());
      case ScalingDecision.ScaleDown down -> log.info("SCALE DOWN -{} instances", down.delta());
      case ScalingDecision.Hold ignored -> log.debug("HOLD — no scaling needed");
    }
  }
}
