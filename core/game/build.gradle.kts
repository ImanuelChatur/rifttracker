plugins {
    alias(libs.plugins.rifttally.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(projects.core.model)
}

// Pure game logic: keep it thoroughly tested. `./gradlew :core:game:koverHtmlReport` shows what's missed.
kover.reports.verify.rule {
    minBound(95)
}

// Part of the usual gate (`testDebugUnitTest`), so a coverage drop fails it.
tasks.named("testDebugUnitTest") { dependsOn("koverVerify") }
