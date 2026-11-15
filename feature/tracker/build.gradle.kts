plugins {
    alias(libs.plugins.rifttally.android.feature)
    alias(libs.plugins.rifttally.android.screenshot)
}

android {
    namespace = "com.rifttally.feature.tracker"
}

dependencies {
    // @IoDispatcher for writing the shared result picture.
    implementation(projects.core.common)
}
