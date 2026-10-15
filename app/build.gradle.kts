plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "pl.example.translator"
    compileSdk = 35
    defaultConfig {
        applicationId = "pl.example.translator"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.4.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
