// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.service) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
    alias(libs.plugins.ksp) apply false
}

// 1. On lit les versions ICI, en dehors du bloc subprojects
val javaVersionStr = libs.versions.java.level.get()
val javaVersionEnum = org.gradle.api.JavaVersion.toVersion(javaVersionStr)

// 2. On applique la configuration
subprojects {
    // Configuration Kotlin
    plugins.withId("org.jetbrains.kotlin.android") {
        val kotlinExt =
            extensions.getByName("kotlin") as org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
        kotlinExt.jvmToolchain(javaVersionStr.toInt())

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(
                    javaVersionStr
                )
            )
        }
    }

    // Configuration Android (Module Application)
    plugins.withId("com.android.application") {
        val androidApp =
            extensions.getByName("android") as com.android.build.api.dsl.ApplicationExtension
        androidApp.compileOptions {
            sourceCompatibility = javaVersionEnum
            targetCompatibility = javaVersionEnum
        }
    }

    // Configuration Android (Modules Library)
    plugins.withId("com.android.library") {
        val androidLib =
            extensions.getByName("android") as com.android.build.api.dsl.LibraryExtension
        androidLib.compileOptions {
            sourceCompatibility = javaVersionEnum
            targetCompatibility = javaVersionEnum
        }
    }
}