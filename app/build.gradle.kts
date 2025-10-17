plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mussaldynerhey.meuleitorrhey"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mussaldynerhey.meuleitorrhey"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.filament.android)
    implementation(libs.media)
    implementation(libs.media3.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")

    implementation("com.google.code.gson:gson:2.8.9")


    implementation(files("libs/acrcloud-universal-sdk-1.3.30.jar"))
    implementation("androidx.core:core-ktx:1.9.0")
}