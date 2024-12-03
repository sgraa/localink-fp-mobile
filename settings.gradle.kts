pluginManagement {
    repositories {
        google { // Define repositories for plugin resolution
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // This ensures no repositories in project-level build files
    repositories {
        google() // Google repository for dependencies
        mavenCentral()
    }
}

rootProject.name = "Localink"
include(":app")
