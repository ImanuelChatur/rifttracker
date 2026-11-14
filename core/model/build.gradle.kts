plugins {
    alias(libs.plugins.rifttally.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Models are @Serializable so :core:database can store configs and events as JSON.
    api(libs.kotlinx.serialization.json)
}
