plugins {
    alias(libs.plugins.android.application)
    //Gooogle services gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.miniproj"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.miniproj"
        minSdk = 26
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.material:material:1.12.0")

    //Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    //Firebase services
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    // Ble services
    implementation ("no.nordicsemi.android:ble:2.9.0") // Adjust the version to the latest
    implementation ("no.nordicsemi.android.support.v18:scanner:1.5.0") // For BLE scanning support

    //graph
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

}