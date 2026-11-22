plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.customboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.customboard.keyboard"
        minSdk = 23
        targetSdk = 35
        versionCode = 101
        versionName = "1.0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
