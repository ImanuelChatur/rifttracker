import java.util.Properties

plugins {
    alias(libs.plugins.rifttally.android.application)
    alias(libs.plugins.rifttally.android.compose)
    alias(libs.plugins.rifttally.hilt)
}

// Release signing comes from the untracked keystore.properties (see README, "Release"). Without it the
// release APK is built unsigned, so the project still builds on any machine.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "com.rifttally.app"

    defaultConfig {
        applicationId = "com.rifttally.app"
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    // LocalIoDispatcher, provided at the root.
    implementation(projects.core.ui)
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
