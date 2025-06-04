package model

data class CameraState(
    val isInitialized: Boolean = false,
    val isCapturing: Boolean = false,
    val error: String? = null,
    val lastCapturedImagePath: String? = null
)