plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mtdevelopment.checkout.presentation"
    compileSdk = 35
    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    flavorDimensions += "version"
    productFlavors {
        create("client") {
            dimension = "version"
        }
        create("admin") {
            dimension = "version"
        }
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {

    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    implementation(project(":checkout:domain"))
    implementation(project(":cart:presentation"))
    implementation(project(":admin:presentation"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.service)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.firebase.crashlytics)

    implementation(libs.google.gson)

    implementation(libs.landscapist)
    implementation(libs.rive.android)
    implementation(libs.lottie)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    implementation(libs.contentment)

    implementation(libs.play.service.wallet)
    implementation(libs.google.pay.compose)
}