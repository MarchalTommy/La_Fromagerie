pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")

        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            // Do not change the username below. It should always be "mapbox" (not your username).
            credentials.username = "mapbox"
            credentials.password = System.getenv("MAPBOX_SECRET_TOKEN") ?: providers.gradleProperty("MAPBOX_SECRET_TOKEN").get()
            authentication.create<BasicAuthentication>("basic")
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
        }
    }
}

rootProject.name = "La Fromagerie"
include(":app")
include(":auth")
include(":core")
include(":core:data")
include(":core:domain")
include(":core:presentation")
include(":cart")
include(":cart:presentation")
include(":home")
include(":home:data")
include(":home:domain")
include(":home:presentation")
include(":details")
include(":details:presentation")
include(":checkout")
include(":checkout:data")
include(":checkout:domain")
include(":checkout:presentation")
include(":admin")
include(":admin:data")
include(":admin:domain")
include(":admin:presentation")
include(":delivery")
include(":delivery:data")
include(":delivery:domain")
include(":delivery:presentation")
include(":cart:domain")
