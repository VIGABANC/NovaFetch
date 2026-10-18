plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.novafetch.library"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
}


dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    implementation(project(":common"))
    implementation(project(":download-core"))
    implementation(libs.kotlinx.coroutines.core)
}
