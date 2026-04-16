rootProject.name = "plugwerk-examples"

// Composite build: each example remains a fully self-contained Gradle build
// with its own settings.gradle.kts and wrapper. This root aggregator only
// delegates lifecycle tasks to the included builds so contributors can run
// `./gradlew build` from the repository root.
includeBuild("plugwerk-java-cli-example")
includeBuild("plugwerk-springboot-thymeleaf-example")
