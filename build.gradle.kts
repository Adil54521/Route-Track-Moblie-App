plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.routetrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.routetrack"
        minSdk = 24
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
    packaging {
        resources {
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.play.services.vision)
    implementation(libs.cronet.embedded)
    implementation(libs.androidx.bluetooth)
    implementation(libs.identity.jvm)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.commons.net.v36)
    implementation (libs.logging.interceptor)
    implementation (libs.jsoup)
    implementation (libs.okhttp)
    implementation (libs.zxing.android.embedded)
    implementation (libs.core)
    implementation (libs.commons.net)
    implementation(libs.kotlinx.serialization.json.v122)
    implementation (libs.androidx.room.runtime)
    implementation (libs.androidx.room.runtime.v250)
    implementation (libs.facebook.shimmer)
    implementation(libs.barcode.scanning)

}
