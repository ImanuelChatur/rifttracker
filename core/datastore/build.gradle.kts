plugins {
    alias(libs.plugins.rifttally.android.library)
    alias(libs.plugins.rifttally.hilt)
}

android {
    namespace = "com.rifttally.core.datastore"
}

dependencies {
    api(libs.androidx.dataStore.preferences)
    api(projects.core.model)
    implementation(projects.core.common)

    testImplementation(libs.kotlinx.coroutines.test)
}
