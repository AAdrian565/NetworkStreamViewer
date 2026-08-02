import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}
val ndiSdkDir = localProperties.getProperty("ndi.sdk.dir")
    ?: error("Add ndi.sdk.dir=/path/to/NDI SDK for Android to local.properties")

val localSigningProperties = Properties().apply {
    val signingFile = rootProject.file("gradle/gradle.properties")
    if (signingFile.isFile) {
        signingFile.inputStream().use { load(it) }
    }
}

fun signingProperty(name: String): String? =
    localSigningProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull

val releaseStoreFile = signingProperty("NETWORKSTREAMVIEWER_STORE_FILE")
val releaseStorePassword = signingProperty("NETWORKSTREAMVIEWER_STORE_PASSWORD")
val releaseKeyAlias = signingProperty("NETWORKSTREAMVIEWER_KEY_ALIAS")
val releaseKeyPassword = signingProperty("NETWORKSTREAMVIEWER_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

if (!hasReleaseSigning && gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
    error(
        "Release signing is not configured. Add NETWORKSTREAMVIEWER_STORE_FILE, " +
            "NETWORKSTREAMVIEWER_STORE_PASSWORD, NETWORKSTREAMVIEWER_KEY_ALIAS, and " +
            "NETWORKSTREAMVIEWER_KEY_PASSWORD to gradle/gradle.properties or ~/.gradle/gradle.properties.",
    )
}

android {
    namespace = "com.adriant.networkstreamviewer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.adriant.networkstreamviewer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments += "-DNDI_SDK_DIR=$ndiSdkDir"
                cppFlags += "-std=c++17"
            }
        }
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    sourceSets {
        getByName("main").jniLibs.directories.add("$ndiSdkDir/lib")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
