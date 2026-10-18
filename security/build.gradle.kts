plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.novafetch.security"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
}
