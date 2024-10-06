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
        versionCode = 1
        versionName = "0.0.1-alpha"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
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
        resources {
            pickFirsts += "androidsupportmultidexversion.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.fragment)

    implementation(libs.androidx.window)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.multidex)
    implementation(libs.bundles.onyx)
    implementation(libs.hiddenapibypass)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
