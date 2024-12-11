plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Apply Google Services plugin
}

android {
    namespace = "com.example.localink"
    compileSdk = 34  // Set to API 29 (maximum you can use)

    defaultConfig {
        applicationId = "com.example.localink"
        minSdk = 33
        targetSdk = 34  // Target SDK can be API 29 as well
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures{
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions:21.1.0")

    // Google Play Services Location for location services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Other dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-video:1.4.0")

    // UI testing dependencies for API 29 compatibility
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Updated to a newer version
    androidTestImplementation("androidx.test:runner:1.5.2") // Updated to a newer version
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1") // Updated to a newer version
    testImplementation("junit:junit:4.13.2")
}

