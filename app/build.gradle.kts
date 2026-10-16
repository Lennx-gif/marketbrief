plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "app.marketbrief"
    compileSdk = 34
    kotlin {
    jvmToolchain(17)
}

    defaultConfig {
        applicationId = "app.marketbrief"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "plan"
    productFlavors {
        create("personal") {
            dimension = "plan"
            buildConfigField("String", "PLAN", "\"PERSONAL\"")
        }
        create("store") {
            dimension = "plan"
            buildConfigField("String", "PLAN", "\"STORE\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }

        packaging {
        resources {
            excludes += "META-INF/*"
        }
    }
    
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:risk"))
    implementation(project(":core:analysis"))
    implementation(project(":core:entitlements"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:news"))
    implementation(project(":feature:calculator"))
    implementation(project(":feature:analysis"))
    implementation(project(":feature:watchlist"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.navigation.fragment)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.workmanager.runtime)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
    kapt(libs.hilt.compiler)
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.compose.testing)
    testImplementation(libs.compose.testing.manifest)
    androidTestImplementation(libs.core.testing)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.testing)
    androidTestImplementation(libs.compose.testing.manifest)
}

kapt {
    correctErrorTypes = true
}

