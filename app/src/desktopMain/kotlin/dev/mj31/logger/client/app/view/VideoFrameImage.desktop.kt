package dev.mj31.logger.client.app.view

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import dev.mj31.logger.client.domain.player.VideoFrame
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

actual fun VideoFrame.toImageBitmap(): ImageBitmap? {
    if (width <= 0 || height <= 0) return null
    val bitmap = Bitmap()
    bitmap.allocPixels(
        imageInfo = ImageInfo(
            width = width,
            height = height,
            colorType = ColorType.BGRA_8888,
            alphaType = ColorAlphaType.UNPREMUL,
        ),
    )
    if (!bitmap.installPixels(pixels = pixels)) return null
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}
