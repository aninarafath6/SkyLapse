package view_model

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import model.CameraCapabilities
import model.CameraEvent
import model.CameraSettings
import model.CameraState
import repo.CameraRepositoryImpl
import repo.I.CameraRepository

class CameraViewModel(
    private val repository: CameraRepository,
    private val cameraId: String
) : ViewModel() {

    private val _cameraState = MutableLiveData(CameraState())
    val cameraState: LiveData<CameraState> = _cameraState

    private val _cameraSettings = MutableLiveData(CameraSettings())
    val cameraSettings: LiveData<CameraSettings> = _cameraSettings

    private val _cameraCapabilities = MutableLiveData<CameraCapabilities>()
    val cameraCapabilities: LiveData<CameraCapabilities> = _cameraCapabilities

    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    init {
        loadCameraCapabilities()
    }

    fun handleEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.InitializeCamera -> initializeCamera()
            is CameraEvent.TakePhoto -> takePhoto()
            is CameraEvent.ToggleManualMode -> toggleManualMode()
            is CameraEvent.SetShutterSpeed -> setShutterSpeed(event.shutterSpeedNanos)
            is CameraEvent.SetISO -> setISO(event.iso)
            is CameraEvent.SetFocusDistance -> setFocusDistance(event.focusDistance)
            is CameraEvent.SwitchCamera -> switchCamera()
        }
    }

    private fun loadCameraCapabilities() {
        _cameraCapabilities.value = repository.getCameraCapabilities(cameraId)
    }

    private fun initializeCamera() {
        viewModelScope.launch {
            try {
                _cameraState.value = _cameraState.value?.copy(isInitialized = false, error = null)

                camera = repository.openCamera(cameraId)
                // Setup ImageReader and capture session
                // This would need to be adapted based on your surface requirements

                _cameraState.value = _cameraState.value?.copy(isInitialized = true)
            } catch (e: Exception) {
                _cameraState.value = _cameraState.value?.copy(error = e.message)
                Log.e("CameraViewModel", "Failed to initialize camera", e)
            }
        }
    }

    private fun takePhoto() {
        val currentCamera = camera ?: return
        val currentSession = captureSession ?: return
        val currentImageReader = imageReader ?: return

        viewModelScope.launch {
            try {
                _cameraState.value = _cameraState.value?.copy(isCapturing = true, error = null)

                val settings = _cameraSettings.value ?: CameraSettings()
                val captureRequest = repository.createCaptureRequest(
                    currentCamera,
                    currentImageReader.surface,
                    settings
                )

                val result = repository.captureStillPicture(currentSession, captureRequest)
                val savedFile = repository.saveImage(result)

                _cameraState.value = _cameraState.value?.copy(
                    isCapturing = false,
                    lastCapturedImagePath = savedFile.absolutePath
                )

            } catch (e: Exception) {
                _cameraState.value = _cameraState.value?.copy(
                    isCapturing = false,
                    error = e.message
                )
                Log.e("CameraViewModel", "Failed to take photo", e)
            }
        }
    }

    private fun toggleManualMode() {
        val currentSettings = _cameraSettings.value ?: CameraSettings()
        val newSettings = currentSettings.copy(isManualMode = !currentSettings.isManualMode)
        _cameraSettings.value = newSettings
        updatePreview()
    }

    private fun setShutterSpeed(shutterSpeedNanos: Long) {
        val capabilities = _cameraCapabilities.value
        val exposureRange = capabilities?.exposureTimeRange

        if (exposureRange != null && shutterSpeedNanos in exposureRange) {
            val currentSettings = _cameraSettings.value ?: CameraSettings()
            val newSettings = currentSettings.copy(shutterSpeed = shutterSpeedNanos)
            _cameraSettings.value = newSettings
            updatePreview()
        }
    }

    private fun setISO(iso: Int) {
        val capabilities = _cameraCapabilities.value
        val isoRange = capabilities?.isoRange

        if (isoRange != null && iso in isoRange) {
            val currentSettings = _cameraSettings.value ?: CameraSettings()
            val newSettings = currentSettings.copy(iso = iso)
            _cameraSettings.value = newSettings
            updatePreview()
        }
    }

    private fun setFocusDistance(focusDistance: Float) {
        val capabilities = _cameraCapabilities.value
        val minFocusDistance = capabilities?.minimumFocusDistance

        if (minFocusDistance != null && focusDistance <= minFocusDistance) {
            val currentSettings = _cameraSettings.value ?: CameraSettings()
            val newSettings = currentSettings.copy(focusDistance = focusDistance)
            _cameraSettings.value = newSettings
            updatePreview()
        }
    }

    private fun switchCamera() {
        // Implementation for switching between front/back camera
        // Would require additional camera ID management
    }

    private fun updatePreview() {
        val currentCamera = camera ?: return
        val currentSession = captureSession ?: return
        val settings = _cameraSettings.value ?: CameraSettings()

        viewModelScope.launch {
            try {
                // This would need the preview surface from the Fragment
                // You might need to pass this as a parameter or handle differently
                // val previewRequest = repository.createPreviewRequest(currentCamera, previewSurface, settings)
                // currentSession.setRepeatingRequest(previewRequest, null, null)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to update preview", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        camera?.close()
        (repository as? CameraRepositoryImpl)?.cleanup()
    }
}