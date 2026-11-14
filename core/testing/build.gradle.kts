plugins {
    alias(libs.plugins.rifttally.android.library)
}

android {
    namespace = "com.rifttally.core.testing"
}

dependencies {
    api(libs.junit)
    api(libs.kotlin.test)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
