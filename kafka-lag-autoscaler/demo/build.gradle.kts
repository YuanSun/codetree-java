description = "kafka-lag-autoscaler demo: runnable Spring Boot app with synthetic lag generation"

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":kafka-lag-autoscaler:spring-boot-starter"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    testImplementation(libs.spring.boot.starter.test)
}
