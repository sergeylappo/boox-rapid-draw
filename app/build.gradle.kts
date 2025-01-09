plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                )
            )
    }
}

android {
    namespace = "com.sergeylappo.booxrapiddraw"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sergeylappo.booxrapiddraw"
        minSdk = 28
        targetSdk = 34
        versionCode = 4
        versionName = "0.0.5-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            //abiFilters += "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
            abiFilters += setOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        jniLibs {
            pickFirsts += "lib/*/libc++_shared.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.fragment)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.window)
    implementation(libs.bundles.onyx)
    implementation(libs.hiddenapibypass)
}
