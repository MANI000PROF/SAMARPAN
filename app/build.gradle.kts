plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.samarpan"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.samarpan"
        minSdk = 25
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase Dependencies
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(platform(libs.firebase.bom))

    implementation(libs.play.services.auth)

    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.play.services.cast.tv)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.compose.material)
    implementation(libs.google.firebase.storage.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")

    implementation("com.github.bumptech.glide:glide:4.13.2")

    implementation("com.cloudinary:cloudinary-android:2.3.1")
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("com.google.mlkit:image-labeling-common:17.0.1")
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    // *Google ML Kit Dependencies for Image Labeling*
    implementation("com.google.mlkit:image-labeling:17.0.7") // Default image labeling
    implementation("com.google.mlkit:image-labeling-custom:17.0.1") // Custom Model support

    // *Google ML Kit Object Detection (For food freshness classification)*
    implementation("com.google.mlkit:object-detection:17.0.1")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")


    // *CameraX Dependencies for Image Capture*
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    implementation("com.cloudinary:cloudinary-android:3.0.2")

    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.google.android.gms:play-services-maps:18.0.0")
    implementation("com.google.android.gms:play-services-location:18.0.0")



}