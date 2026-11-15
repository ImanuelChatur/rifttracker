plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = libs.composeRules.ktlint.get().toString()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .customRuleSets(listOf(composeRules))
            .editorConfigOverride(
                mapOf(
                    // Design-token locals provided by RiftTheme.
                    "compose_allowed_composition_locals" to "LocalRiftSpacing,LocalRiftSizes,LocalRiftScoreTypography",
                ),
            )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
    }
    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**", ".idea/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
