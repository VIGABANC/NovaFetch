plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "app.novafetch"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.novafetch"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":download-core"))
    implementation(project(":download-android"))
    implementation(project(":library"))
    implementation(project(":player"))
    implementation(project(":security"))
}
