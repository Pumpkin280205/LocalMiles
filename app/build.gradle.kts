import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Values that must not be committed (client IDs, keys). Pulled from local.properties.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "com.example.localmiles"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.localmiles"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google/Firebase Web client ID used as serverClientId for Credential Manager.
        // Set GOOGLE_WEB_CLIENT_ID in local.properties (see Firebase console -> Auth -> Google -> Web SDK config).
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""}\"",
        )

        // API base URL. Defaults to 10.0.2.2, the emulator's alias for the host machine's
        // localhost, so it works out of the box when running the API on your own PC.
        // On a physical device (or if the API runs elsewhere), set API_BASE_URL in
        // local.properties to your machine's LAN IP, e.g.:
        //   API_BASE_URL=http://192.168.1.23:3000/api/
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localProps.getProperty("API_BASE_URL") ?: "//192.168.1.23:3000/api/"}\"",
        )
    }

    // Shared debug keystore (app/debug.keystore) so every developer's debug build
    // has the same SHA-1, which is what's registered in the Firebase console.
    // Without this, Gradle falls back to each machine's own auto-generated
    // ~/.android/debug.keystore, and Google sign-in fails with an unregistered SHA-1.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig= true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    //added for the LocalMiles app

    // Compose (usually already present in a new project template)
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Networking — talking to AJ's Express API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Offline storage (Sayali's sync layer)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")   // annotation processor

    // Background sync
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Maps (Dispatcher live map, Driver route, Client tracking)
    implementation("com.google.maps.android:maps-compose:6.4.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // OpenStreetMap tiles for the dispatcher Live Map — no API key needed, so
    // the map renders for every developer and marker out of the box.
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Image loading — POD photos, profile images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Firebase — BOM first so the artifacts below need no explicit versions.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")   // push notifications
    implementation("com.google.firebase:firebase-auth")            // Google sign-in -> Firebase ID token

    // Google Sign-In (current recommended approach — Credential Manager, not the older GoogleSignInClient)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    // Lets us await() Play Services / Firebase Task<T> from coroutines.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Biometric auth
    implementation("androidx.biometric:biometric:1.1.0")

    // Dependency injection (optional but recommended for a 4-person codebase)
    implementation("com.google.dagger:hilt-android:2.53")
    ksp("com.google.dagger:hilt-compiler:2.53")
}