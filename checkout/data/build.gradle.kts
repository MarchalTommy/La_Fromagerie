plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mtdevelopment.checkout.data"
    compileSdk = 35
    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        val SUMUP_PRIVATE_KEY =
            System.getenv("SUMUP_PRIVATE_KEY") ?: project.findProperty("SUMUP_PRIVATE_KEY")
                ?.toString()
        buildConfigField("String", "SUMUP_PRIVATE_KEY", "\"$SUMUP_PRIVATE_KEY\"")

        val SUMUP_PUBLIC_KEY =
            System.getenv("SUMUP_PUBLIC_KEY") ?: project.findProperty("SUMUP_PUBLIC_KEY")
                ?.toString()
        buildConfigField("String", "SUMUP_PUBLIC_KEY", "\"$SUMUP_PUBLIC_KEY\"")

        val SUMUP_MERCHANT_ID =
            System.getenv("SUMUP_MERCHANT_ID") ?: project.findProperty("SUMUP_MERCHANT_ID")
                ?.toString()
        buildConfigField("String", "SUMUP_MERCHANT_ID", "\"$SUMUP_MERCHANT_ID\"")

        val SUMUP_MERCHANT_ID_TEST = System.getenv("SUMUP_MERCHANT_ID_TEST") ?: project.findProperty("SUMUP_MERCHANT_ID_TEST")
            ?.toString()
        buildConfigField("String", "SUMUP_MERCHANT_ID_TEST", "\"$SUMUP_MERCHANT_ID_TEST\"")

        val GOOGLE_PAY_PROFILE_ID =
            System.getenv("GOOGLE_PAY_PROFILE_ID") ?: project.findProperty("GOOGLE_PAY_PROFILE_ID")
            ?.toString()
        buildConfigField("String", "GOOGLE_PAY_PROFILE_ID", "\"$GOOGLE_PAY_PROFILE_ID\"")

        val GOOGLE_PAY_MERCHANT_ID = System.getenv("GOOGLE_PAY_MERCHANT_ID") ?: project.findProperty("GOOGLE_PAY_MERCHANT_ID")
            ?.toString()
        buildConfigField("String", "GOOGLE_PAY_MERCHANT_ID", "\"$GOOGLE_PAY_MERCHANT_ID\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
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

    implementation(project(":core:data"))
    implementation(project(":core:domain"))
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
    implementation(libs.ktor.content.negociation)
    implementation(libs.ktor.serialization.json)

    implementation(libs.mapbox.geojson)

    implementation(libs.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
}