package model

import android.hardware.camera2.CameraMetadata

data class CameraSettings(
    val isManualMode: Boolean = false,
    val shutterSpeed: Long = 1000000000L / 60, // 1/60 second in nanoseconds
    val iso: Int = 100,
    val focusDistance: Float = 0f, // 0 means infinity focus
    val whiteBalance: Int = CameraMetadata.CONTROL_AWB_MODE_AUTO
)