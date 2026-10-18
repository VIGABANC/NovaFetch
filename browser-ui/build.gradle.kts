plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "app.novafetch.browser.ui"
    compileSdk = 36
    buildFeatures { compose = true }
    defaultConfig { minSdk = 29 }
}
dependencies {
    implementation(project(":browser-core"))
    implementation(project(":media-resolver"))
    implementation(project(":media-detection"))
    implementation(project(":download-core"))
    implementation(libs.geckoview)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
}
