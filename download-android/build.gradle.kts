plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.novafetch.download.android"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
}

dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.work.testing)
    implementation(project(":common"))
    implementation(project(":download-core"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
