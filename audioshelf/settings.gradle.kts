pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "audioshelf"

// Pure-Kotlin domain logic lives in its own build so it can be built and
// tested without the Android SDK: `./gradlew -p core test`.
includeBuild("core")
include(":app")
