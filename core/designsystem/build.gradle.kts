plugins {
    alias(libs.plugins.rifttally.android.library)
    alias(libs.plugins.rifttally.android.compose)
    alias(libs.plugins.rifttally.android.screenshot)
}

android {
    namespace = "com.rifttally.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.iconsCore)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
