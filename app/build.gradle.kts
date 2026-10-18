plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.novafetch"
    compileSdk = 36

    buildFeatures { compose = true }

    defaultConfig {
        applicationId = "app.novafetch"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    implementation(project(":media-resolver"))
    implementation(project(":media-detection"))
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":download-core"))
    implementation(project(":download-android"))
    implementation(project(":library"))
    implementation(project(":player"))
    implementation(project(":security"))
    implementation(project(":browser-core"))
    implementation(project(":browser-ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
}
