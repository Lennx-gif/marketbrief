rootProject.name = "marketbrief"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Pure-Kotlin domain logic. No Android dependencies, so it is fast to test and reusable anywhere.
include(":core:model")
include(":core:risk")
include(":core:analysis")
include(":core:entitlements")
include(":core:data")
include(":core:storage")
include(":core:ui")

// Feature modules
include(":feature:news")
include(":feature:calculator")
include(":feature:analysis")
include(":feature:watchlist")
include(":feature:settings")

// App module
include(":app")

