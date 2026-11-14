import androidx.room.gradle.RoomExtension
import com.rifttally.buildlogic.lib
import com.rifttally.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                // Schemas are committed so every version has a migration test.
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "implementation"(libs.lib("androidx.room.runtime"))
                "implementation"(libs.lib("androidx.room.ktx"))
                "ksp"(libs.lib("androidx.room.compiler"))
                "androidTestImplementation"(libs.lib("androidx.room.testing"))
            }
        }
    }
}
