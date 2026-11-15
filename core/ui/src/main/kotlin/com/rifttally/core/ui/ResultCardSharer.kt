package com.rifttally.core.ui

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shares a [ResultCard] as a PNG through the Android share sheet. Create it with
 * [rememberResultCardSharer] and call [share] from a coroutine (e.g. `rememberCoroutineScope()`).
 *
 * The card is drawn off-screen at a fixed size, density and font scale, so the picture is the same
 * [ResultCardImageWidthPx] × [ResultCardImageHeightPx] on every phone. The PNG goes to
 * `cacheDir/shared_results/` and is handed out through the app's `FileProvider` (authority
 * `"<packageName>.fileprovider"`, declared in `:app`). Nothing touches the network.
 */
public class ResultCardSharer internal constructor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private var isSharing = false

    /**
     * Renders [state], writes the PNG and opens the share sheet. Returns `false` (and does nothing more)
     * when a share is already running, the picture couldn't be drawn or written, or no `FileProvider`
     * is set up. Must be called on the main thread.
     */
    public suspend fun share(state: ResultCardState): Boolean {
        if (isSharing) return false // a double tap shares once
        isSharing = true
        try {
            val activity = context.findActivity() ?: return false
            val portraits = loadPortraits(activity, state.imageUris)
            val image = renderOffScreen(activity, state, portraits) ?: return false
            val file = try {
                withContext(ioDispatcher) { writeResultPng(shareDirectory(activity), image.asAndroidBitmap()) }
            } catch (e: IOException) {
                return false
            }
            val uri = try {
                FileProvider.getUriForFile(activity, fileProviderAuthority(activity), file)
            } catch (e: IllegalArgumentException) {
                // No FileProvider for this authority / path in the manifest.
                return false
            }
            activity.startActivity(
                Intent.createChooser(resultShareIntent(uri), activity.getString(R.string.core_ui_result_share_title)),
            )
            return true
        } finally {
            isSharing = false
        }
    }
}

/** A [ResultCardSharer] for the current screen; [ioDispatcher] writes the PNG (inject `@IoDispatcher`). */
@Composable
public fun rememberResultCardSharer(ioDispatcher: CoroutineDispatcher): ResultCardSharer {
    val context = LocalContext.current
    return remember(context, ioDispatcher) { ResultCardSharer(context, ioDispatcher) }
}

/** Size of the shared PNG in pixels. */
public val ResultCardImageWidthPx: Int get() = (ResultCardWidth.value * CARD_DENSITY).roundToInt()
public val ResultCardImageHeightPx: Int get() = (ResultCardHeight.value * CARD_DENSITY).roundToInt()

/** `"<packageName>.fileprovider"`: the authority `:app` declares its `FileProvider` with. */
internal fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

/** Where shared PNGs go: `cacheDir/shared_results/` (the path `:app`'s FileProvider XML exposes). */
internal fun shareDirectory(context: Context): File = File(context.cacheDir, SHARE_DIRECTORY)

/**
 * Writes [bitmap] as a new PNG in [directory] (created if needed) and deletes older PNGs there, so the
 * cache never holds more than the latest card. Returns the new file.
 */
@Throws(IOException::class)
internal fun writeResultPng(directory: File, bitmap: Bitmap): File {
    if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Can't create $directory")
    val file = File(directory, "result-${UUID.randomUUID()}.png")
    file.outputStream().use { out ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)) throw IOException("Can't encode PNG")
    }
    directory.listFiles()?.filter { it != file && it.name.endsWith(".png") }?.forEach { it.delete() }
    return file
}

/** The `ACTION_SEND` intent for a PNG at [uri], with read access granted to the receiving app. */
internal fun resultShareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND)
    .setType(PNG_MIME_TYPE)
    .putExtra(Intent.EXTRA_STREAM, uri)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    .apply { clipData = ClipData.newRawUri(null, uri) }

/** Loads every portrait as a software bitmap (drawable into the off-screen layer), skipping failures. */
private suspend fun loadPortraits(context: Context, uris: List<String>): ImmutableMap<String, ImageBitmap> {
    val loader = SingletonImageLoader.get(context)
    return uris.mapNotNull { uri ->
        val request = ImageRequest.Builder(context).data(uri).allowHardware(false).build()
        val result = loader.execute(request) as? SuccessResult ?: return@mapNotNull null
        uri to result.image.toBitmap().asImageBitmap()
    }.toMap().toImmutableMap()
}

/**
 * Composes the card in an invisible view behind the screen's content, records its first frame into a
 * graphics layer without drawing it, and returns that frame. `null` if nothing was drawn in time (e.g.
 * the screen went off).
 */
internal suspend fun renderOffScreen(
    activity: Activity,
    state: ResultCardState,
    portraits: ImmutableMap<String, ImageBitmap>,
): ImageBitmap? {
    val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return null
    val frame = CompletableDeferred<ImageBitmap>()
    val view = ComposeView(activity).apply {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(CARD_DENSITY, fontScale = 1f)) {
                CardRecorder(state = state, portraits = portraits, onRecord = { frame.complete(it) })
            }
        }
    }
    root.addView(view, 0, ViewGroup.LayoutParams(ResultCardImageWidthPx, ResultCardImageHeightPx))
    return try {
        withTimeoutOrNull(RENDER_TIMEOUT_MILLIS) { frame.await() }
    } finally {
        // Not right away: this can resume inside the frame that recorded the card, and removing a child
        // while its parent is drawing crashes the draw pass.
        root.post { root.removeView(view) }
    }
}

/** Draws nothing on screen: the card goes into a graphics layer that is turned into a bitmap once. */
@Composable
private fun CardRecorder(
    state: ResultCardState,
    portraits: ImmutableMap<String, ImageBitmap>,
    onRecord: (ImageBitmap) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val recorded = remember { AtomicBoolean(false) }
    Box(
        modifier = Modifier.drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            if (recorded.compareAndSet(false, true)) {
                scope.launch { onRecord(layer.toImageBitmap()) }
            }
        },
    ) {
        ResultCard(state = state, portraits = portraits)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Pixels per dp of the shared picture: 1080 × 1350 px. */
private const val CARD_DENSITY = 3f
private const val SHARE_DIRECTORY = "shared_results"
private const val PNG_MIME_TYPE = "image/png"
private const val PNG_QUALITY = 100
private const val RENDER_TIMEOUT_MILLIS = 5_000L
