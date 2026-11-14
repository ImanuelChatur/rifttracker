import com.android.build.api.dsl.ApplicationExtension
import com.rifttally.buildlogic.JAVA_VERSION
import com.rifttally.buildlogic.configureKotlin
import com.rifttally.buildlogic.libs
import com.rifttally.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.versionInt("compileSdk")
                defaultConfig {
                    minSdk = libs.versionInt("minSdk")
                    targetSdk = libs.versionInt("targetSdk")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                lint {
                    abortOnError = true
                    checkDependencies = false
                }
            }
            configureKotlin()
        }
    }
}
