plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.novafetch.library"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
}


dependencies {
    implementation(project(":common"))
    implementation(project(":download-core"))
    implementation(libs.kotlinx.coroutines.core)
}
