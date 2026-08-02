plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {
    jvm("desktop") {
        withJava()
        mainRun {
            mainClass = "dev.mj31.logger.client.app.MainKt"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":domain"))
                implementation(project(":data"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlin.inject.runtime)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.truth)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

/** Every user visible string lives in `composeResources`, so a translation is a new values folder. */
compose.resources {
    publicResClass = true
    packageOfResClass = "dev.mj31.logger.client.app.resources"
    generateResClass = auto
}

/** kotlin-inject generates the graph for the JVM target; the component itself stays in commonMain. */
dependencies {
    add("kspDesktop", libs.kotlin.inject.compiler)
}

/**
 * Native installers accept a purely numeric version (`MAJOR[.MINOR][.PATCH]`, major greater than
 * zero) on every platform, so a pre-release qualifier cannot be stored in jpackage metadata at all.
 *
 * The product version keeps the qualifier; the installer carries its numeric core, which is the same
 * release it belongs to.
 */
fun installerVersion(version: String): String = version.substringBefore(delimiter = '-')

/** Name shared by the application bundle and by the installer artefact. */
val appPackageName = "MJLogs"

/**
 * Publishes the installer under the full product version.
 *
 * jpackage can only name its output after the numeric version it accepts, so two pre-releases of the
 * same release would produce the very same file name. The bundle metadata stays untouched: the copy
 * only restores the qualifier in the artefact humans hand around.
 */
tasks.register<Copy>("dmg") {
    description = "Builds the macOS installer and names it after the full product version."
    group = "distribution"
    dependsOn("packageDmg")

    val installerName = "$appPackageName-${project.version}.dmg"
    from(layout.buildDirectory.dir("compose/binaries/main/dmg")) {
        include("*.dmg")
        rename { installerName }
    }
    into(layout.buildDirectory.dir("distributions"))

    doLast { logger.lifecycle("The installer is written to ${destinationDir.resolve(installerName)}") }
}

compose.desktop {
    application {
        mainClass = "dev.mj31.logger.client.app.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = appPackageName
            packageVersion = installerVersion(version = project.version.toString())

            // jpackage links a minimal runtime; JavaCPP, and therefore the bundled FFmpeg decoder,
            // needs sun.misc.Unsafe from jdk.unsupported. Without it the packaged app starts but
            // cannot decode a single frame.
            modules("jdk.unsupported")

            // LGPL requires the licence to travel with the binaries it covers.
            appResourcesRootDir.set(layout.projectDirectory.dir("legal"))

            // The same drawing in the three containers the platforms insist on.
            macOS {
                bundleID = "dev.mj31.logger.client"
                iconFile.set(project.file("icons/icon.icns"))
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
