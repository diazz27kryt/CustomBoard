plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.customboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.customboard"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-alpha"
    }
}
