import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.rifttally.buildlogic.lib
import com.rifttally.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { buildFeatures.compose = true }
            }

            dependencies {
                val bom = libs.lib("androidx.compose.bom")
                "implementation"(platform(bom))
                "androidTestImplementation"(platform(bom))
                "implementation"(libs.lib("androidx.compose.ui.tooling.preview"))
                "debugImplementation"(libs.lib("androidx.compose.ui.tooling"))
                "debugImplementation"(libs.lib("androidx.compose.ui.test.manifest"))
                "androidTestImplementation"(libs.lib("androidx.compose.ui.test.junit4"))
                // Compose ui-test pulls in an Espresso too old for API 37 (InputManager.getInstance removed).
                "androidTestImplementation"(libs.lib("androidx.test.espresso.core"))
            }
        }
    }
}
