import com.android.build.api.dsl.LibraryExtension
import com.rifttally.buildlogic.JAVA_VERSION
import com.rifttally.buildlogic.configureKotlin
import com.rifttally.buildlogic.lib
import com.rifttally.buildlogic.libs
import com.rifttally.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.versionInt("compileSdk")
                defaultConfig {
                    minSdk = libs.versionInt("minSdk")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                testOptions.targetSdk = libs.versionInt("targetSdk")
                lint {
                    targetSdk = libs.versionInt("targetSdk")
                    abortOnError = true
                }
            }
            configureKotlin()

            dependencies {
                "testImplementation"(libs.lib("junit"))
                "testImplementation"(libs.lib("kotlin.test"))
                "androidTestImplementation"(libs.lib("androidx.test.ext.junit"))
                "androidTestImplementation"(libs.lib("androidx.test.runner"))
            }
        }
    }
}
