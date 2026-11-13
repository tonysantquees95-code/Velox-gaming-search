plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val numeroDaVersao = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "tech.vetorial.velox"
    compileSdk = 35

    defaultConfig {
        applicationId = "tech.vetorial.velox"
        resValue("string", "app_name", "Velox")
        minSdk = 26
        targetSdk = 34
        versionCode = numeroDaVersao
        versionName = "1.$numeroDaVersao"
    }

    // O APK sai assinado com a chave de depuração do Gradle e é assinado de novo, no CI, com a chave permanente (assinar-apk.sh).
    buildTypes {
        debug { isMinifyEnabled = false }
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    testImplementation("junit:junit:4.13.2")
}
