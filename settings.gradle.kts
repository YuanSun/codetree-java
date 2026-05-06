rootProject.name = "codetree-java"

include(
    "kafka-lag-autoscaler:core",
    "kafka-lag-autoscaler:backend-kafka",
    "kafka-lag-autoscaler:metrics-otel",
    "kafka-lag-autoscaler:spring-boot-starter",
    "kafka-lag-autoscaler:demo",
)
