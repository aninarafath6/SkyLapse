package model

import android.hardware.camera2.CaptureResult
import android.media.Image
import java.io.Closeable

data class CombinedCaptureResult(
    val image: Image,
    val metadata: CaptureResult,
    val orientation: Int,
    val format: Int,
    val cameraId: String
) : Closeable {
    override fun close() = image.close()
}