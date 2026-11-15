package com.rifttally.core.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.lifecycleScope
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.contrastRatio
import java.io.File
import java.nio.file.Files
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The share helper's file, intent and off-screen rendering, plus the card's color contrast. */
@RunWith(RobolectricTestRunner::class)
class ResultCardSharerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun writeResultPng_writesAPng_andKeepsOnlyTheLatest() {
        val directory = File(Files.createTempDirectory("share").toFile(), "shared_results")
        val bitmap = Bitmap.createBitmap(4, 5, Bitmap.Config.ARGB_8888)

        val first = writeResultPng(directory, bitmap)
        val second = writeResultPng(directory, bitmap)

        assertTrue(second.exists())
        assertEquals(false, first.exists())
        assertEquals(listOf(second), directory.listFiles()?.toList())
        val decoded = assertNotNull(BitmapFactory.decodeFile(second.absolutePath))
        assertEquals(4 to 5, decoded.width to decoded.height)
    }

    @Test
    fun shareIntent_sendsThePng_withReadAccess() {
        val uri = Uri.parse("content://com.rifttally.app.fileprovider/shared_results/result.png")

        val intent = resultShareIntent(uri)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        @Suppress("DEPRECATION")
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
    }

    @Test
    fun authorityAndDirectory_matchWhatAppDeclares() {
        val context = composeRule.activity

        assertEquals("${context.packageName}.fileprovider", fileProviderAuthority(context))
        assertEquals(File(context.cacheDir, "shared_results"), shareDirectory(context))
    }

    @Test
    fun renderOffScreen_drawsTheCardAtTheFixedPixelSize() {
        // A screen needs content (and its view-tree owners) for the off-screen view to compose.
        composeRule.setContent { Text("Screen") }
        var image: ImageBitmap? = null
        composeRule.activity.lifecycleScope.launch {
            image = renderOffScreen(composeRule.activity, previewResultCardState, persistentMapOf())
        }

        waitForMainThread { image != null }

        val result = assertNotNull(image)
        assertEquals(ResultCardImageWidthPx to ResultCardImageHeightPx, result.width to result.height)
        assertEquals(1080 to 1350, result.width to result.height)
    }

    @Test
    fun share_withoutAFileProvider_returnsFalse_insteadOfCrashing() {
        // The library's test manifest declares no FileProvider: the share stops before the share sheet.
        composeRule.setContent { Text("Screen") }
        val results = mutableListOf<Boolean>()
        val sharer = ResultCardSharer(composeRule.activity, Dispatchers.Unconfined)
        composeRule.activity.lifecycleScope.launch { results += sharer.share(previewResultCardState) }
        // A second tap while the first share runs is ignored.
        composeRule.activity.lifecycleScope.launch { results += sharer.share(previewResultCardState) }

        waitForMainThread { results.size == 2 }

        assertEquals(listOf(false, false), results)
        assertEquals(1, shareDirectory(composeRule.activity).listFiles()?.size)
    }

    @Test
    fun playerColors_keepAaContrastOnBothRowContainers() {
        var containers: List<androidx.compose.ui.graphics.Color> = emptyList()
        composeRule.setContent { ReadContainers { containers = it } }

        containers.forEach { container ->
            PlayerColors.all.forEach { color ->
                val ratio = contrastRatio(color.color, container)
                assertTrue(ratio >= AA, "${color.id} on $container: $ratio")
            }
        }
        assertEquals(2, containers.size)
    }

    /**
     * Runs the main looper in 16 ms steps until [condition] holds: the off-screen frame and the snapshot
     * resume through posted, frame-timed callbacks that a plain idle doesn't reach in Robolectric.
     */
    private fun waitForMainThread(condition: () -> Boolean) {
        repeat(MAX_FRAMES) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MILLIS))
        }
        assertTrue(condition(), "Not done after $MAX_FRAMES frames")
    }

    @Composable
    private fun ReadContainers(onRead: (List<androidx.compose.ui.graphics.Color>) -> Unit) {
        RiftTheme(darkTheme = true) {
            onRead(listOf(RiftTheme.colorScheme.surfaceContainerLow, RiftTheme.colorScheme.surfaceContainer))
        }
    }

    private companion object {
        const val AA = 4.5
        const val MAX_FRAMES = 600
        const val FRAME_MILLIS = 16L
    }
}
