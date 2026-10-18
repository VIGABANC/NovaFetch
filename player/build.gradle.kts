plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.novafetch.player"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
}
