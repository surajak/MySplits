// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
    //alias(libs.plugins.compose.compiler) apply false
}
/*
android {
    signingConfigs {
        release {
            storeFile file(keystoreFile())
            storePassword keystorePassword()
            keyAlias keyAlias()
            keyPassword keyPassword()
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            isMinifyEnabled true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
*/