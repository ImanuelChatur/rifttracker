plugins {
    alias(libs.plugins.rifttally.android.library)
}

android {
    namespace = "com.rifttally.core.testing"
}

dependencies {
    // Fakes implement the public repository interfaces from :core:data.
    api(projects.core.data)
    api(libs.junit)
    api(libs.kotlin.test)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
