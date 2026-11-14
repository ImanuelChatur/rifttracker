import com.rifttally.buildlogic.JAVA_VERSION
import com.rifttally.buildlogic.configureKotlin
import com.rifttally.buildlogic.lib
import com.rifttally.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            // Android Lint also checks pure-Kotlin modules when running `./gradlew lint`.
            pluginManager.apply("com.android.lint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            configureKotlin()

            dependencies {
                "testImplementation"(libs.lib("junit"))
                "testImplementation"(libs.lib("kotlin.test"))
            }

            // Lets `./gradlew testDebugUnitTest` also run pure-JVM module tests.
            tasks.register("testDebugUnitTest") {
                group = "verification"
                description = "Runs JVM unit tests (alias of `test` for Android-style task names)."
                dependsOn("test")
            }
        }
    }
}
