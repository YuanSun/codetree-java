description = "kafka-lag-autoscaler metrics-otel: OpenTelemetry gauges and counters"

dependencies {
    api(project(":kafka-lag-autoscaler:core"))
    api(libs.opentelemetry.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
