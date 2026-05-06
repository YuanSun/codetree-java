description = "kafka-lag-autoscaler spring-boot-starter: zero-config Spring Boot auto-configuration"

plugins {
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
    }
}

dependencies {
    api(project(":kafka-lag-autoscaler:core"))
    implementation(project(":kafka-lag-autoscaler:backend-kafka"))
    implementation(project(":kafka-lag-autoscaler:metrics-otel"))

    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly("org.slf4j:slf4j-api")
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
}
