plugins {
    alias(libs.plugins.rifttally.android.library)
    alias(libs.plugins.rifttally.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rifttally.core.data"
}

dependencies {
    api(projects.core.model)
    // Repositories expose GameEvent and GameEventFactory to features.
    api(projects.core.game)
    // GameRecord.toStatsGame() feeds the history and stats features.
    api(projects.core.stats)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    // Legend catalog (Phase 1B) and the event-log JSON payloads (Phase 2).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
