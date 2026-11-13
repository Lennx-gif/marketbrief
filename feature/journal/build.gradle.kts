plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")
}

android { namespace = "app.marketbrief.feature.journal"; compileSdk = 34
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
    kotlin { jvmToolchain(17) }
}

dependencies {
    implementation(project(":core:risk"))
    implementation(project(":core:data"))
    implementation(project(":core:storage"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit)
}
