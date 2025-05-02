plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.WeatherApp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.WeatherApp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // JSON persistence
    implementation("com.google.code.gson:gson:2.12.1")
    // Firebase BOM – manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    // Core Firebase products
    implementation("com.google.firebase:firebase-analytics")
    // ← Add this to pull in the Realtime Database SDK:
    implementation("com.google.firebase:firebase-database")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.android.gms:play-services-location:18.0.0")
    implementation ("com.google.android.gms:play-services-maps:18.1.0")
    implementation(libs.swiperefreshlayout)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.android.libraries.places:places:4.2.0")
}