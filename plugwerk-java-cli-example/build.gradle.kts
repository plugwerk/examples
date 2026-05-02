// Shared conventions for all modules of the CLI example.

plugins {
    id("com.diffplug.spotless") version "8.4.0" apply false
}

val lifecycleTasks = listOf("build", "clean", "assemble", "check")
lifecycleTasks.forEach { taskName ->
    tasks.register(taskName) {
        group = "build"
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

    // While the SDK is on SNAPSHOTs, always re-resolve changing modules so CI
    // and local builds pick up upstream fixes within minutes instead of waiting
    // for Gradle's 24h default cache. Drop this once the examples pin to a
    // stable plugwerk release.
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
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
