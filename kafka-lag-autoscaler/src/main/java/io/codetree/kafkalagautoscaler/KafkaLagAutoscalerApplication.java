package io.codetree.kafkalagautoscaler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KafkaLagAutoscalerApplication {

  public static void main(String[] args) {
    SpringApplication.run(KafkaLagAutoscalerApplication.class, args);
  }
}
