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
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "La Fromagerie"
include(":app")
include(":auth")
include(":admin")
include(":core")
include(":cart")
include(":checkout")
include(":home")
include(":core:data")
include(":core:domain")
include(":core:presentation")
include(":home:data")
include(":home:domain")
include(":home:presentation")
