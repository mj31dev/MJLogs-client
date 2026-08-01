plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
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

tasks.register("detektFormat") {
    description = "Reformats code according to Detekt 2.0 formatting rules across all subprojects."
    group = "formatting"
    dependsOn("detekt")
}

tasks.register("test") {
    description = "Runs multiplatform unit tests across all modules."
    group = "verification"
    dependsOn(
        ":domain:desktopTest",
        ":data:desktopTest",
        ":app:desktopTest",
    )
}
