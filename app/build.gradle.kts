plugins {
    alias(libs.plugins.rifttally.android.application)
    alias(libs.plugins.rifttally.android.compose)
    alias(libs.plugins.rifttally.hilt)
}

android {
    namespace = "com.rifttally.app"

    defaultConfig {
        applicationId = "com.rifttally.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.history)
    implementation(projects.feature.home)
    implementation(projects.feature.settings)
    implementation(projects.feature.setup)
    implementation(projects.feature.stats)
    implementation(projects.feature.tracker)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
