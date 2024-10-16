plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mtdevelopment.checkout.data"
    compileSdk = 34
    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        val SUMUP_PRIVATE_KEY: String by project
        buildConfigField("String", "SUMUP_PRIVATE_KEY", "\"$SUMUP_PRIVATE_KEY\"")
        val SUMUP_PUBLIC_KEY: String by project
        buildConfigField("String", "SUMUP_PUBLIC_KEY", "\"$SUMUP_PUBLIC_KEY\"")
        val SUMUP_MERCHANT_ID: String by project
        buildConfigField("String", "SUMUP_MERCHANT_ID", "\"$SUMUP_MERCHANT_ID\"")
        val GOOGLE_PAY_ID: String by project
        buildConfigField("String", "GOOGLE_PAY_ID", "\"$GOOGLE_PAY_ID\"")
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

    implementation(project(":core:data"))
    implementation(project(":checkout:domain"))

    implementation(libs.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.service)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    implementation(libs.play.service.wallet)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)

    implementation(libs.datastore.preferences)
}