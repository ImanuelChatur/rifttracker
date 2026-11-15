plugins {
    alias(libs.plugins.rifttally.android.feature)
    alias(libs.plugins.rifttally.android.screenshot)
}

android {
    namespace = "com.rifttally.feature.stats"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.stats)
}
