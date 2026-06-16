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
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            credentials {
                username = System.getenv("JITPACK_TOKEN")
                    ?: providers.gradleProperty("JITPACK_TOKEN").orNull.orEmpty()
            }
        }

        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            // Do not change the username below. It should always be "mapbox" (not your username).
            credentials.username = "mapbox"
            // Resolved leniently so builds relying on cached artifacts still configure when
            // the token is absent; downloads from Mapbox will then fail with 401 instead.
            credentials.password = System.getenv("MAPBOX_SECRET_TOKEN")
                ?: providers.gradleProperty("MAPBOX_SECRET_TOKEN").orNull.orEmpty()
            authentication.create<BasicAuthentication>("basic")
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
