plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.maelnor"
version = "0.0.1-SNAPSHOT"
description = "OzonBomgeBot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-community-dialects")


    implementation("org.telegram:telegrambots-springboot-longpolling-starter:9.2.0")
    implementation("org.telegram:telegrambots-client:9.2.0")
    implementation("org.telegram:telegrambots-abilities:9.2.0")
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("com.microsoft.playwright:playwright:1.56.0")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.flywaydb:flyway-core:11.14.1")
    implementation("org.jfree:jfreechart:1.5.6")


    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "ru.maelnor.ozonbomgebot.OzonBomgeBotApplication"
    }
}