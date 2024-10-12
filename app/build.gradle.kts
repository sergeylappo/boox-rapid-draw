plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.sergeylappo.booxrapiddraw"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sergeylappo.booxrapiddraw"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "0.0.2-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            //abiFilters += "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
            abiFilters += "arm64-v8a"
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
    implementation("androidx.core:core-ktx:1.13.1")

    implementation(libs.androidx.window)
    implementation(libs.bundles.onyx)
    implementation(libs.hiddenapibypass)
}
