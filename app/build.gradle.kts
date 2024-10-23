plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mtdevelopment.lafromagerie"
    compileSdk = 34

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEYSTORE_ALIAS") ?: project.findProperty("KEYSTORE_ALIAS")
                ?.toString()
            println(keyAlias)
            keyPassword =
                System.getenv("KEYSTORE_ALIAS_PASS") ?: project.findProperty("KEYSTORE_ALIAS_PASS")
                    ?.toString()
            println(keyPassword)
            storeFile =
                (System.getenv("KEYSTORE_PATH") ?: project.findProperty("KEYSTORE_PATH"))?.let {
                    file(
                        it
                    )
                }
            println(storeFile)
            storePassword =
                System.getenv("KEYSTORE_PASS") ?: (project.findProperty("KEYSTORE_PASS") as? String)
            println(storePassword)
        }
    }
    defaultConfig {
        applicationId = "com.mtdevelopment.lafromagerie"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    packaging {
        jniLibs.pickFirsts.add("**/libc++_shared.so")
    }
}

dependencies {

    implementation(project(":core:presentation"))
    implementation(project(":cart:presentation"))
    implementation(project(":home:presentation"))
    implementation(project(":details:presentation"))

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
    implementation(libs.navigation.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
}