plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.fitpulse.mobile"
    compileSdk = 36

    defaultConfig {
        // The applicationId MUST match the Wear OS module so both apps can talk
        // over the Wearable Data Layer (MessageClient / DataClient).
        applicationId = "com.example.fitpulse"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Wearable Data Layer (MessageClient + DataClient + NodeClient).
    implementation(libs.play.services.wearable)
    // .await() coroutine extensions so we can call Wearable APIs sequentially.
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.phone.material3)
    implementation(libs.activity.compose)
}
