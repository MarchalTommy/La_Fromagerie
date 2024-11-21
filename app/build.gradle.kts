plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.service)
    alias(libs.plugins.firebase.crashlytics.plugin)
    alias(libs.plugins.ksp)
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
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.1.0"

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

    flavorDimensions += "version"
    productFlavors {
        create("client") {
            dimension = "version"
            versionNameSuffix = "-client"
        }
        create("admin") {
            dimension = "version"
            versionNameSuffix = "-admin"
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
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":cart:presentation"))
    implementation(project(":home:presentation"))
    implementation(project(":home:domain"))
    implementation(project(":home:data"))
    implementation(project(":details:presentation"))
    implementation(project(":checkout:presentation"))
    implementation(project(":checkout:domain"))
    implementation(project(":checkout:data"))

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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.navigation.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
    testImplementation(libs.koin.test)

    implementation(libs.play.service.wallet)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.content.negociation)
    implementation(libs.ktor.serialization.json)

    implementation(libs.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
}

class Version(code: Int, version: String) {
    private var major: Int = 0
    private var minor: Int = 0
    private var patch: Int = 0
    private var code: Int = code

    init {
        val (major, minor, patch) = version.split(".")
        this.major = major.toInt()
        this.minor = minor.toInt()
        this.patch = patch.toInt()
    }

    fun bumpMajor() {
        major += 1
        minor = 0
        patch = 0
        code += 1
    }

    fun bumpMinor() {
        minor += 1
        patch = 0
        code += 1
    }

    fun bumpPatch() {
        patch += 1
        code += 1
    }

    fun getName(): String = "$major.$minor.$patch"

    fun getCode(): Int = code
}

tasks.addRule("Pattern: bump<TYPE>Version") {
    val taskName = this
    if (taskName.matches("bump(Major|Minor|Patch)Version".toRegex())) {
        tasks.create(taskName) {
            doLast {
                val type = taskName.removePrefix("bump").removeSuffix("Version")

                println("Bumping ${type.lowercase()} version...")

                val oldVersionCode = android.defaultConfig.versionCode!!
                val oldVersionName = android.defaultConfig.versionName!!

                val version = Version(oldVersionCode, oldVersionName)
                when (type) {
                    "Major" -> version.bumpMajor()
                    "Minor" -> version.bumpMinor()
                    "Patch" -> version.bumpPatch()
                }

                val newVersionName = version.getName()
                val newVersionCode = version.getCode()

                println("$oldVersionName ($oldVersionCode) → $newVersionName ($newVersionCode)")

                // Update version properties in buildFile
                val updated = buildFile.readText()
                    .replaceFirst(
                        "versionName = \"$oldVersionName\"",
                        "versionName = \"$newVersionName\""
                    )
                    .replaceFirst("versionCode = $oldVersionCode", "versionCode = $newVersionCode")
                buildFile.writeText(updated)
            }
        }
    }
}