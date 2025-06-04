package model

import android.util.Range

data class CameraCapabilities(
    val supportsManualControls: Boolean,
    val isoRange: Range<Int>?,
    val exposureTimeRange: Range<Long>?,
    val minimumFocusDistance: Float?
)