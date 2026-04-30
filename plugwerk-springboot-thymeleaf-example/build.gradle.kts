// Spring Boot + Thymeleaf example host application with dynamic PF4J plugin pages.

plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.1.0"
}

val lifecycleTasks = listOf("build", "clean", "assemble", "check")
lifecycleTasks.forEach { taskName ->
    tasks.named(taskName) {
        dependsOn(subprojects.mapNotNull { it.tasks.findByName(taskName) })
        if (taskName == "build" || taskName == "assemble") {
            dependsOn(copyClientPlugin)
        }
    }
}

val plugwerkClientPluginZip: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    plugwerkClientPluginZip(
        variantOf(libs.plugwerk.client.plugin) {
            classifier("pf4j")
            artifactType("zip")
        },
    )
}

val copyClientPlugin by tasks.registering(Copy::class) {
    group = "build"
    description = "Downloads the plugwerk-client-plugin PF4J ZIP into the plugins directory"
    from(plugwerkClientPluginZip)
    into(layout.projectDirectory.dir("plugins"))
}

val projectVersion = rootProject.file("../VERSION").readText().trim()

allprojects {
    group = "io.plugwerk.examples"
    version = projectVersion

    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/plugwerk/plugwerk")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .getOrElse("")
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .getOrElse("")
            }
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            licenseHeaderFile(rootProject.file("../license-header.txt"))
            googleJavaFormat()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

spotless {
    java {
        target("src/**/*.java")
        licenseHeaderFile(rootProject.file("../license-header.txt"))
        googleJavaFormat()
    }
}

dependencies {
    // Extension-point API
    implementation(project(":plugwerk-springboot-thymeleaf-example-api"))

    // plugwerk-spi must be on the HOST classpath so PF4J can match the interface
    // loaded by the parent classloader with the plugin's implementation.
    // plugwerk-client-plugin itself is NOT a compile dependency —
    // it is loaded at runtime as a PF4J plugin from the plugins directory.
    implementation(libs.plugwerk.spi)
    implementation(libs.pf4j)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    // Spring Boot + Thymeleaf
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation(libs.thymeleaf.layout.dialect)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

springBoot {
    mainClass.set("io.plugwerk.example.webapp.WebApp")
}
