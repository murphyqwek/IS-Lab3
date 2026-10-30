import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Lab1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.apache.commons:commons-dbcp2")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<BootRun>("bootRunDev") {
    group = "application"
    description = "Runs application with the dev Spring profile"

    classpath = sourceSets["main"].runtimeClasspath

    mainClass.set(
        tasks.named<BootRun>("bootRun")
            .flatMap { it.mainClass }
    )

    args("--spring.profiles.active=dev")
}

tasks.register<Exec>("dockerComposeUp") {
    group = "infrastructre"
    description = "Run docker compose up to run a container with postgresql"
    workingDir(projectDir)
    commandLine("docker", "compose", "up", "-d")
}

tasks.register<Exec>("dockerComposeDown") {
    group = "infrastructre"
    description = "Run docker compose down to run down a container with postgresql"
    workingDir(projectDir)
    commandLine("docker", "compose", "down")
}

tasks.register<Exec>("compileFrontend") {
    group = "infrastructure"
    description = "Compile Angular"

    workingDir = file("frontend/frontend")

    environment(
        "PATH",
        "/home/murphyqwek/.nvm/versions/node/v24.21.0/bin:" +
                System.getenv("PATH")
    )

    commandLine("npm", "run", "build")
}

tasks.register<Copy>("copyFrontend") {
    dependsOn("compileFrontend")

    from("frontend/frontend/dist/frontend/browser")
    into("src/main/resources/static")
}

tasks.named("processResources") {
    dependsOn("copyFrontend")
}
