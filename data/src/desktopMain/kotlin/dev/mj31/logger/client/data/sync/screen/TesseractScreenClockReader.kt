package dev.mj31.logger.client.data.sync.screen

import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader
import java.io.File
import dev.mj31.logger.client.domain.sync.screen.ScreenClockTime
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.tesseract.TessBaseAPI

/**
 * [ScreenClockReader] backed by the Tesseract engine bundled with the application.
 *
 * Everything happens on this machine: there is no service to call, no key to hold and nothing to
 * upload, which is the same promise the bundled decoder already makes about the video itself.
 *
 * The recognizer is told the crop is a single line of text and nothing more. Narrowing it to digits
 * would be the obvious next step, and does not work: the neural engine of Tesseract 4 and later
 * ignores a character whitelist. What comes back is therefore filtered by [ClockTextParser] instead,
 * which is where the tolerance for a misread colon or a letter-shaped digit lives.
 *
 * A [TessBaseAPI] handle is not thread safe, so every call must arrive on the one thread that scans
 * the video — the same rule the frame grabber imposes, and for the same reason.
 */
class TesseractScreenClockReader(
    dataDirectory: File?,
    private val preparer: ClockImagePreparer = ClockImagePreparer(),
    private val parser: ClockTextParser = ClockTextParser(),
    private val pageSegMode: Int = PAGE_SEG_SPARSE_TEXT,
) : ScreenClockReader {

    private val api: TessBaseAPI? = openApi(dataDirectory = dataDirectory)

    override val isAvailable: Boolean
        get() = api != null

    override fun read(frame: VideoFrame, region: ClockRegion): ScreenClockTime? {
        val handle = api ?: return null
        val image = preparer.prepare(frame = frame, region = region) ?: return null
        val text = runCatching { recognize(handle = handle, image = image) }.getOrNull() ?: return null
        return parser.parse(text = text)
    }

    override fun release() {
        api?.let { handle ->
            runCatching { handle.End() }
            runCatching { handle.close() }
        }
    }

    private fun recognize(handle: TessBaseAPI, image: ClockImage): String? {
        val buffer = BytePointer(*image.pixels)
        try {
            handle.SetImage(buffer, image.width, image.height, BYTES_PER_GREY_PIXEL, image.width)
            val recognized = handle.GetUTF8Text() ?: return null
            return try {
                recognized.string
            } finally {
                recognized.deallocate()
            }
        } finally {
            buffer.deallocate()
        }
    }

    /**
     * Returns `null` rather than throwing when the model is absent: a distribution that shipped
     * without it still plays video and still synchronizes by hand, it only cannot read a clock.
     */
    private fun openApi(dataDirectory: File?): TessBaseAPI? {
        val directory = dataDirectory?.takeIf { File(it, TRAINED_DATA).isFile } ?: return null
        val handle = TessBaseAPI()
        val initialized = runCatching { handle.Init(directory.absolutePath, LANGUAGE) == 0 }.getOrDefault(false)
        if (!initialized) {
            runCatching { handle.close() }
            return null
        }
        handle.SetPageSegMode(pageSegMode)
        return handle
    }

    companion object {
        private const val LANGUAGE = "eng"

        /**
         * `Init` takes the directory the model actually sits in and does not append `tessdata` to
         * it, whatever the environment variable of that name suggests.
         */
        private const val TRAINED_DATA = "eng.traineddata"

        private const val BYTES_PER_GREY_PIXEL = 1

        /**
         * `PSM_SPARSE_TEXT` of Tesseract's `PageSegMode`: find text wherever it is, in no particular
         * order and with no assumption that it forms a paragraph.
         *
         * A status bar is not one line. Arrive in an app from another one and iOS puts a back chip
         * beside the clock and shifts the clock along; the crop then holds two unrelated fragments,
         * and a recognizer told to expect a single line makes one mangled string out of them.
         */
        const val PAGE_SEG_SPARSE_TEXT = 11

    }
}
