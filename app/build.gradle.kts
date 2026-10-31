import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val sideloadKeyAvailable: Boolean =
    !System.getenv("MORSEPRENDE_KEYSTORE_B64").isNullOrBlank() &&
        !System.getenv("MORSEPRENDE_KEYSTORE_PASSWORD").isNullOrBlank()

android {
    namespace = "io.github.egt17baa.morseprende"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.egt17baa.morseprende"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
    }

    // Clave permanente para los APK de entrega. Vive en variables de entorno (nunca en el repo):
    // MORSEPRENDE_KEYSTORE_B64 = almacén PKCS12 en base64, MORSEPRENDE_KEYSTORE_PASSWORD = contraseña.
    signingConfigs {
        create("sideload") {
            if (sideloadKeyAvailable) {
                val keystore = layout.buildDirectory.file("signing/sideload.p12").get().asFile
                keystore.parentFile.mkdirs()
                keystore.writeBytes(Base64.getDecoder().decode(System.getenv("MORSEPRENDE_KEYSTORE_B64").trim()))
                storeFile = keystore
                storeType = "PKCS12"
                storePassword = System.getenv("MORSEPRENDE_KEYSTORE_PASSWORD")
                keyAlias = "morseprende"
                keyPassword = System.getenv("MORSEPRENDE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (sideloadKeyAvailable) signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Esquemas de Room para probar migraciones con Robolectric (solo debug; no van en el APK de entrega).
    sourceSets["debug"].assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric descarga android-all por su cuenta: se usa el espejo de Google de Maven Central.
            it.systemProperty(
                "robolectric.dependency.repo.url",
                "https://maven-central.storage-download.googleapis.com/maven2/",
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":morse-core"))
    implementation(project(":game"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}

// El APK de entrega debe ir siempre firmado con la clave permanente: sin ella, mejor fallar que
// entregar un APK que no se podría instalar encima del anterior.
tasks.matching { it.name == "assembleRelease" || it.name == "packageRelease" }.configureEach {
    doFirst {
        if (!sideloadKeyAvailable) {
            throw GradleException(
                "Faltan MORSEPRENDE_KEYSTORE_B64 / MORSEPRENDE_KEYSTORE_PASSWORD: no se puede firmar el APK de entrega",
            )
        }
    }
}
