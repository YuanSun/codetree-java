description = "kafka-lag-autoscaler backend-kafka: LagProvider backed by KafkaAdminClient"

dependencies {
    api(project(":kafka-lag-autoscaler:core"))
    api(libs.kafka.clients)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
