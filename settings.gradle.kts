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
        maven("https://maven.mozilla.org/maven2/")
    }
}

rootProject.name = "NovaFetch"
include(
    ":app",
    ":common",
    ":data",
    ":download-core",
    ":download-android",
    ":library",
    ":player",
    ":security",
)
