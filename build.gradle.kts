// Root aggregator for the composite build declared in settings.gradle.kts.
//
// Each example is an independent Gradle build (own settings.gradle.kts, own
// wrapper, own version catalog). This file exists only to wire root-level
// lifecycle tasks to the matching root tasks in each included build.

val includedBuildNames = listOf(
    "plugwerk-java-cli-example",
    "plugwerk-springboot-thymeleaf-example",
)

val aggregateTasks = listOf("build", "clean", "assemble", "check")

aggregateTasks.forEach { taskName ->
    tasks.register(taskName) {
        group = "build"
        description = "Runs :$taskName in every included example build"
        dependsOn(
            includedBuildNames.map { name ->
                gradle.includedBuild(name).task(":$taskName")
            },
        )
    }
}
