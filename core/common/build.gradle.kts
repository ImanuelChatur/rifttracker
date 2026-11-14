plugins {
    alias(libs.plugins.rifttally.android.library)
    alias(libs.plugins.rifttally.hilt)
}

android {
    namespace = "com.rifttally.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
