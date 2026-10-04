plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.plugin.aiassist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.plugin.aiassist"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

// PluginBuilder block — names the output .cgp file
pluginBuilder {
    pluginName = "ai-assist"
    // pluginVersion left unset → auto-generated timestamp version
}

dependencies {
    // Plugin API — provided by the IDE host at runtime
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // UI
    implementation(libs.material)

    // Networking (OpenAI-compatible API calls)
    implementation(libs.okhttp)
    implementation(libs.gson)

    // Coroutines for background threading
    implementation(libs.coroutines.android)
}