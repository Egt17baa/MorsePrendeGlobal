plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// La prueba de la ficha de Play lee este fichero: se declara como entrada para que se repita al cambiarlo.
tasks.test {
    val ficha = rootProject.file("docs/publicacion/ficha-google-play.md")
    inputs.file(ficha).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("ficha")
    systemProperty("ficha.path", ficha.absolutePath)
    val politica = rootProject.file("docs/publicacion/politica-privacidad.md")
    inputs.file(politica).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("politica")
    systemProperty("politica.path", politica.absolutePath)
    // RealContentTest lee los JSON de contenido de la app: si cambian, las pruebas se vuelven a ejecutar.
    inputs.dir(rootProject.file("app/src/main/assets/content")).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("contenido")
}

dependencies {
    implementation(project(":morse-core"))
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
