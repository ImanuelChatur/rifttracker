plugins {
    alias(libs.plugins.rifttally.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)
}
