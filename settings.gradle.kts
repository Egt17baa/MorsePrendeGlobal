pluginManagement {
    repositories {
        google()
        // Espejo oficial de Google de Maven Central: evita los límites de peticiones (429) de repo.maven.apache.org.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Espejo oficial de Google de Maven Central: evita los límites de peticiones (429) de repo.maven.apache.org.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}
rootProject.name = "MorsePrende"
include(":morse-core", ":app")
