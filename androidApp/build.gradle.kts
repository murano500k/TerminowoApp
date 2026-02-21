plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
}

fun localProperty(key: String, default: String = ""): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return default
    return file.readLines()
        .filter { it.contains("=") && !it.startsWith("#") }
        .associate {
            val (k, v) = it.split("=", limit = 2)
            k.trim() to v.trim()
        }[key] ?: default
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.google.play.services.auth)

            // Koin for Android
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
        }
    }
}

android {
    namespace = "com.stc.terminowo.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.stc.terminowo.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "PROXY_URL", "\"${localProperty("PROXY_URL")}\"")
        buildConfigField("String", "PROXY_API_KEY", "\"${localProperty("PROXY_API_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFilePath = localProperty("KEYSTORE_FILE")
            storeFile = if (keystoreFilePath.isNotEmpty()) file(keystoreFilePath) else null
            storePassword = localProperty("KEYSTORE_PASSWORD")
            keyAlias = localProperty("KEY_ALIAS")
            keyPassword = localProperty("KEY_PASSWORD")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
