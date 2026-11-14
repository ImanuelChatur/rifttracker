import com.android.build.api.dsl.LibraryExtension
import com.rifttally.buildlogic.lib
import com.rifttally.buildlogic.libs
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Roborazzi screenshot tests on the JVM (Robolectric, native graphics).
 *
 * - `./gradlew recordRoborazziDebug` records baselines into `src/test/screenshots/`.
 * - `./gradlew verifyRoborazziDebug` compares against them.
 * - Plain `testDebugUnitTest` runs the tests without recording or comparing.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }

            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
            }

            tasks.withType<Test>().configureEach {
                systemProperty("robolectric.graphicsMode", "NATIVE")
                systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                systemProperty("roborazzi.record.filePathStrategy", "relativePathFromRoborazziContextOutputDirectory")
                maxHeapSize = "2g"
                // Robolectric/Hilt generate test sources even in modules with no tests yet.
                failOnNoDiscoveredTests.set(false)
            }

            dependencies {
                val bom = libs.lib("androidx.compose.bom")
                "testImplementation"(platform(bom))
                "testImplementation"(libs.lib("androidx.compose.ui.test.junit4"))
                // Compose ui-test pulls in an Espresso too old for API 37 (InputManager.getInstance removed).
                "testImplementation"(libs.lib("androidx.test.espresso.core"))
                "testImplementation"(libs.lib("robolectric"))
                "testImplementation"(libs.lib("roborazzi"))
                "testImplementation"(libs.lib("roborazzi.compose"))
                "testImplementation"(libs.lib("roborazzi.junit.rule"))
            }
        }
    }
}
