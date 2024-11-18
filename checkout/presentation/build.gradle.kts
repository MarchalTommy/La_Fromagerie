plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mtdevelopment.checkout.presentation"
    compileSdk = 34
    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        val MAPBOX_PUBLIC_TOKEN =
            System.getenv("MAPBOX_PUBLIC_TOKEN") ?: project.findProperty("MAPBOX_PUBLIC_TOKEN")
                ?.toString()
        val MAPBOX_SECRET_TOKEN =
            System.getenv("MAPBOX_SECRET_TOKEN") ?: project.findProperty("MAPBOX_SECRET_TOKEN")
                ?.toString()
        buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"$MAPBOX_PUBLIC_TOKEN\"")
        buildConfigField(
            "String", "MAPBOX_SECRET_TOKEN",
            "\"$MAPBOX_SECRET_TOKEN\""
        )
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
}

dependencies {

    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    implementation(project(":checkout:domain"))
    implementation(project(":cart:presentation"))

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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.firebase.crashlytics)

    implementation(libs.landscapist)
    implementation(libs.rive.android)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    implementation(libs.mapbox)
    implementation(libs.mapbox.extension)

    implementation(libs.location)
    implementation(libs.accompanist.permissions)

    implementation(libs.contentment)

    implementation(libs.play.service.wallet)
    implementation(libs.google.pay.compose)
}