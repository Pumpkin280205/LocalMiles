package com.example.localmiles.Screens.Driver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import java.io.File
import kotlin.math.max

private const val MAX_PHOTO_SIDE = 1600

// Where the camera saves the POD photo
fun podPhotoFile(context: Context): File =
    File(context.cacheDir, "pod").apply { mkdirs() }.let { File(it, "photo.jpg") }

// Makes the photo smaller so the sync request stays small (the API limit is 25 MB).
// Returns null if the file isn't an image
fun downscalePhoto(source: File): File? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.path, bounds)
    if (bounds.outWidth <= 0) return null

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_SIDE) sample *= 2
    val bitmap = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null

    val out = File(source.parentFile, "photo_small.jpg")
    out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    return out
}

// One stroke = all the points of one finger drag
typealias Strokes = SnapshotStateList<List<Offset>>

// A box the recipient signs in with their finger
@Composable
fun SignaturePad(strokes: Strokes, onSize: (IntSize) -> Unit, modifier: Modifier = Modifier) {
    val current = remember { mutableStateOf<List<Offset>>(emptyList()) }

    ComposeCanvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(ComposeColor.White)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .onSizeChanged(onSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { current.value = listOf(it) },
                    onDrag = { change, _ -> current.value = current.value + change.position },
                    onDragEnd = { strokes.add(current.value); current.value = emptyList() },
                    onDragCancel = { current.value = emptyList() },
                )
            }
    ) {
        (strokes + listOf(current.value)).forEach { points ->
            if (points.size > 1) {
                val path = ComposePath().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                // pad is always white so the ink is always black
                drawPath(path, ComposeColor.Black, style = Stroke(width = 4f))
            }
        }
    }
}

// Turns the strokes into a PNG (black on white). Null if nothing was drawn
fun renderSignature(context: Context, strokes: List<List<Offset>>, size: IntSize): File? {
    if (strokes.none { it.size > 1 } || size.width <= 0 || size.height <= 0) return null

    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.filter { it.size > 1 }.forEach { points ->
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, paint)
    }

    val out = File(File(context.cacheDir, "pod").apply { mkdirs() }, "signature.png")
    out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return out
}
