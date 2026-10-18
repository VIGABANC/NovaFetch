plugins { alias(libs.plugins.android.library) }
android {
    namespace = "app.novafetch.browser.core"
    compileSdk = 36
    defaultConfig { minSdk = 29; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
}
dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    implementation(project(":media-detection"))
    implementation(project(":media-resolver"))
    implementation(project(":security"))
    implementation(libs.geckoview)
    implementation(libs.kotlinx.coroutines.core)
}
