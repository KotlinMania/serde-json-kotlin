pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

val localMavenValidation =
    providers.gradleProperty("project.repositories.mavenLocal")
        .map { it.trim().toBooleanStrict() }
        .getOrElse(false)

dependencyResolutionManagement {
    if (localMavenValidation) {
        val localMavenPath = providers.systemProperty("maven.repo.local").orNull
        require(localMavenPath != null && File(localMavenPath).isAbsolute) {
            "Local Maven validation requires -Dmaven.repo.local=/absolute/path/to/isolated-repository"
        }
        // Kotlin's Node/Yarn setup adds project repositories. Keep their
        // distributions available while routing dependencies through settings.
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    }
    repositories {
        if (localMavenValidation) {
            exclusiveContent {
                forRepository { mavenLocal { name = "kotlinManiaLocalValidation" } }
                filter { includeGroup("io.github.kotlinmania") }
            }
        }
        google()
        mavenCentral()
        if (localMavenValidation) {
            ivy("https://nodejs.org/dist") {
                name = "Node Distributions"
                patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                metadataSources { artifact() }
                content { includeModule("org.nodejs", "node") }
            }
            ivy("https://github.com/yarnpkg/yarn/releases/download") {
                name = "Yarn Distributions"
                patternLayout { artifact("v[revision]/[artifact]-v[revision].[ext]") }
                metadataSources { artifact() }
                content { includeModule("com.yarnpkg", "yarn") }
            }
        }
    }
}

rootProject.name = "serde-json-kotlin"
