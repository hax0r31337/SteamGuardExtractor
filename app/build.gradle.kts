plugins {
    id("com.android.application")
}

android {
    namespace = "dev.sora.extractor"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.sora.extractor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        create("sign") {
            storeFile = rootProject.file("signing.jks")
            keyAlias = "sora"
            keyPassword = "1145141919"
            storePassword = "1145141919"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sign")
        }
        debug {
            signingConfig = signingConfigs.getByName("sign")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}