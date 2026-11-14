plugins {
    alias(libs.plugins.rifttally.android.library)
    alias(libs.plugins.rifttally.android.compose)
    alias(libs.plugins.rifttally.android.screenshot)
}

android {
    namespace = "com.rifttally.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    // LegendAvatar loads portraits from assets (Phase 1B). No network module: v1 is offline.
    implementation(libs.coil.compose)
    // Window insets controller for immersive mode.
    implementation(libs.androidx.core.ktx)
}
