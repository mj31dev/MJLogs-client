/**
 * JavaCPP ships one native bundle per platform. Only the host one is pulled in: the full set would
 * add well over a hundred megabytes to every build for no benefit.
 */
val javacppPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isArm = arch.contains("aarch64") || arch.contains("arm")
    when {
        os.contains("mac") -> if (isArm) "macosx-arm64" else "macosx-x86_64"
        os.contains("win") -> "windows-x86_64"
        else -> if (isArm) "linux-arm64" else "linux-x86_64"
    }
}

/** `group:name:version` of a catalog entry, as a plain coordinate. */
fun coordinateOf(dependency: Provider<MinimalExternalModuleDependency>): String {
    val module = dependency.get()
    return "${module.module.group}:${module.module.name}:${module.versionConstraint.requiredVersion}"
}

/** Native bundle of a catalog entry, built for [javacppPlatform]. */
fun nativesOf(dependency: Provider<MinimalExternalModuleDependency>): String =
    "${coordinateOf(dependency = dependency)}:$javacppPlatform"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 * Room writes the schema of every version here, and reads them back to generate the migrations.
 *
 * The directory is committed on purpose: a migration is derived by comparing the new schema with the
 * previous one, so a schema that only ever existed on one machine cannot be migrated away from.
 */
room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":domain"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }

        val desktopMain by getting {
            dependencies {
                // Room and its bundled SQLite. The driver is JVM only, which is why every persistent
                // repository lives in this source set rather than in `commonMain`.
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                // JavaCV declares every preset it can possibly wrap; only the FFmpeg one is wanted.
                implementation(coordinateOf(dependency = libs.javacv)) { isTransitive = false }
                implementation(libs.ffmpeg)
                implementation(nativesOf(dependency = libs.ffmpeg))
                implementation(nativesOf(dependency = libs.javacpp))

                // Reads the clock a screencast displays. Leptonica is Tesseract's image layer and
                // is named explicitly so that its native bundle is pinned to the host platform too.
                implementation(libs.tesseract)
                implementation(nativesOf(dependency = libs.tesseract))
                implementation(libs.leptonica)
                implementation(nativesOf(dependency = libs.leptonica))
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
                implementation(libs.room.testing)
            }
        }
    }
}

/** Room generates its implementations for the JVM target only; there is no other target to serve. */
dependencies {
    add("kspDesktop", libs.room.compiler)
}
