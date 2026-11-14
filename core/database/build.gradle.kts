plugins {
    alias(libs.plugins.rifttally.android.library)
    alias(libs.plugins.rifttally.android.room)
    alias(libs.plugins.rifttally.hilt)
}

android {
    namespace = "com.rifttally.core.database"
    sourceSets {
        // Exported schemas are test assets for MigrationTestHelper.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

dependencies {
    implementation(projects.core.common)

    androidTestImplementation(libs.kotlinx.coroutines.test)
}
