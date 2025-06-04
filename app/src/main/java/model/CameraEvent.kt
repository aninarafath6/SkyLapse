package model

sealed class CameraEvent {
    object InitializeCamera : CameraEvent()
    object TakePhoto : CameraEvent()
    object ToggleManualMode : CameraEvent()
    data class SetShutterSpeed(val shutterSpeedNanos: Long) : CameraEvent()
    data class SetISO(val iso: Int) : CameraEvent()
    data class SetFocusDistance(val focusDistance: Float) : CameraEvent()
    object SwitchCamera : CameraEvent()
}