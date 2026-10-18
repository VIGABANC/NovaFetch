plugins { alias(libs.plugins.android.library) }
android {
    namespace = "app.novafetch.media.resolver"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
}
dependencies {
    implementation(project(":media-detection"))
    implementation(project(":download-core"))
    implementation(project(":security"))
}
