import kotlin.text.lowercase
import kotlin.text.removePrefix
import kotlin.text.removeSuffix

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
        versionCode = 2
        versionName = "1.0.1"

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
                    .replaceFirst("versionName = \"$oldVersionName\"", "versionName = \"$newVersionName\"")
                    .replaceFirst("versionCode = $oldVersionCode", "versionCode = $newVersionCode")
                buildFile.writeText(updated)
            }
        }
    }
}