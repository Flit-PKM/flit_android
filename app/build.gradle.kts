import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: set RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD.
// If RELEASE_STORE_FILE is unset, defaults to $HOME/android_keystores/flit-release.keystore.
// When the keystore exists and all four are set, release builds are signed; otherwise release is unsigned.
val releaseStoreFile: File = System.getenv("RELEASE_STORE_FILE")?.let { file(it) }
    ?: file("${System.getenv("HOME") ?: ""}/android_keystores/flit-release.keystore")
val releaseStorePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
val hasReleaseSigning = releaseStoreFile.exists() &&
    releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

android {
    namespace = "com.bmdstudios.flit"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.bmdstudios.flit"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Browser URL for Flit Core web login (independent of BACKEND_BASE_URL; debug API may be local).
        buildConfigField(
            "String",
            "FLIT_CORE_WEB_LOGIN_URL",
            "\"https://core.flit-pkm.com/?redirect=login\""
        )
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"https://core.flit-pkm.com/terms\""
        )

        externalNativeBuild{
            cmake{
                cppFlags("-std=c++17")
            }
        }
        ndk{
            abiFilters += listOf("arm64-v8a")
        }
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // Default: emulator (10.0.2.2) or same host. Override via local.properties or env if needed.
            buildConfigField("String", "BACKEND_BASE_URL", "\"http://192.168.10.121:8000\"")
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // mapping.txt (Play deobfuscation) is only produced when isMinifyEnabled is true.
            // Release is currently unminified; enable isMinifyEnabled and add keep rules if desired.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://core.flit-pkm.com\"")
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.okhttp)
    implementation(libs.timber)
    implementation(libs.onnxruntime.android)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Compose RichText for Markdown rendering
    implementation(libs.compose.richtext.commonmark)
    implementation(libs.compose.richtext.ui.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
