plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    autoCorrect = true
    source.setFrom(files("domain/src", "data/src", "app/src"))
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

/**
 * Upper bound on how many Kotlin files may sit in one directory.
 *
 * Detekt cannot express this: its rules visit one file at a time and know nothing about the
 * directory around it, so the layout is checked by the build instead.
 */
val maxFilesPerDirectory = 5

tasks.register("verifySourceLayout") {
    description = "Fails when a source directory holds more than $maxFilesPerDirectory Kotlin files."
    group = "verification"

    val sourceRoots = subprojects.map { it.projectDir.resolve("src") }
    val projectRoot = rootDir

    doLast {
        val violations = sourceRoots
            .filter { it.exists() }
            .flatMap { root -> root.walkTopDown().filter { it.isDirectory }.toList() }
            .mapNotNull { directory ->
                val count = directory.listFiles().orEmpty().count { it.isFile && it.extension == "kt" }
                if (count > maxFilesPerDirectory) "  ${directory.relativeTo(projectRoot)}: $count files" else null
            }
            .sorted()

        if (violations.isNotEmpty()) {
            throw GradleException(
                "At most $maxFilesPerDirectory Kotlin files are allowed per directory; " +
                    "split these into sub-packages:\n" + violations.joinToString(separator = "\n"),
            )
        }
    }
}

/**
 * One task, not two.
 *
 * `autoCorrect` is on, so this reformats what the formatting ruleset can fix and fails on what it
 * cannot: a long function, a magic number, a positional argument. A second task that only checked
 * would run the very same analysis, so the pair `format` then `check` was one command twice.
 */
tasks.named("detekt") { dependsOn("verifySourceLayout") }

tasks.register("test") {
    description = "Runs multiplatform unit tests across all modules."
    group = "verification"
    dependsOn(
        ":domain:desktopTest",
        ":data:desktopTest",
        ":app:desktopTest",
    )
}
