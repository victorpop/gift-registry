plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)

    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.giftregistry"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.giftregistry"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            // Default to "true" so the dev loop (./gradlew :app:assembleDebug) keeps hitting the emulator.
            // Opt out for on-device testing with: ./gradlew :app:assembleDebug -Puse_emulator=false
            val useEmulator = providers.gradleProperty("use_emulator").getOrElse("true")
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulator)
        }
        release {
            // Hardcoded false — release builds MUST NEVER point at the emulator,
            // regardless of whether -Puse_emulator is passed. This is a safety gate.
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "false")
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.ui.text.google.fonts)
    implementation("androidx.activity:activity-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")

    // Navigation3
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20")
    implementation(libs.hilt.navigation.compose)

    // Firebase (main modules -- NOT ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.messaging)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // DataStore
    implementation(libs.datastore.preferences)

    // AppCompat (for locale switching)
    implementation(libs.appcompat)

    // Coil 3 (image loading for Compose)
    // coil-network-okhttp is REQUIRED — Coil 3 split networking into a separate module.
    // Without it, HTTP image URLs silently fail to load.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
