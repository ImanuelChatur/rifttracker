import com.rifttally.buildlogic.lib
import com.rifttally.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("rifttally.android.library")
            pluginManager.apply("rifttally.android.compose")
            pluginManager.apply("rifttally.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:model"))

                "implementation"(libs.lib("androidx.navigation.compose"))
                "implementation"(libs.lib("androidx.hilt.lifecycle.viewmodel.compose"))
                "implementation"(libs.lib("androidx.lifecycle.runtime.compose"))
                "implementation"(libs.lib("androidx.lifecycle.viewmodel.compose"))
                "implementation"(libs.lib("androidx.compose.material3"))
                "implementation"(libs.lib("kotlinx.serialization.json"))
                "implementation"(libs.lib("kotlinx.collections.immutable"))

                "testImplementation"(project(":core:testing"))
                "testImplementation"(libs.lib("kotlinx.coroutines.test"))
                "testImplementation"(libs.lib("turbine"))
            }
        }
    }
}
