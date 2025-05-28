plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.service)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mtdevelopment.admin.data"
    compileSdk = 35
    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        val CLOUDINARY_URL =
            System.getenv("CLOUDINARY_URL") ?: project.findProperty("CLOUDINARY_URL")
                ?.toString()
        buildConfigField("String", "CLOUDINARY_URL", "\"$CLOUDINARY_URL\"")

        val CLOUDINARY_PUBLIC =
            System.getenv("CLOUDINARY_PUBLIC") ?: project.findProperty("CLOUDINARY_PUBLIC")
                ?.toString()
        buildConfigField("String", "CLOUDINARY_PUBLIC", "\"$CLOUDINARY_PUBLIC\"")

        val CLOUDINARY_PRIVATE =
            System.getenv("CLOUDINARY_PRIVATE") ?: project.findProperty("CLOUDINARY_PRIVATE")
                ?.toString()
        buildConfigField("String", "CLOUDINARY_PRIVATE", "\"$CLOUDINARY_PRIVATE\"")

        val GOOGLE_API =
            System.getenv("GOOGLE_API") ?: project.findProperty("GOOGLE_API")
                ?.toString()
        buildConfigField("String", "GOOGLE_API", "\"$GOOGLE_API\"")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":admin:domain"))

    implementation(libs.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    implementation(libs.cloudinary)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
    testImplementation(libs.koin.test)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.content.negociation)
    implementation(libs.ktor.serialization.json)

    implementation(libs.protobuf.lite)
    implementation(libs.location)
}