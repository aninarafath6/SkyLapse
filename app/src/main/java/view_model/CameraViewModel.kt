package view_model

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.util.Log
import android.view.Surface
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
    private val cameraId: String,
    private val pixelFormat: Int
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
    private var previewSurface: Surface? = null


    companion object {
        private const val IMAGE_BUFFER_SIZE = 3
    }

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
        val surface = previewSurface ?: return

        viewModelScope.launch {
            try {
                _cameraState.value = _cameraState.value?.copy(isInitialized = false, error = null)

                camera = repository.openCamera(cameraId)
                val cameraManager = repository.getCameraManager()
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val size =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(pixelFormat)
                        .maxByOrNull { it.height * it.width }!!
                imageReader = ImageReader.newInstance(
                    size.width,
                    size.height,
                    pixelFormat,
                    IMAGE_BUFFER_SIZE
                )

                // Create capture session with both preview surface and image reader surface
                val targets = listOf(surface, imageReader!!.surface)
                captureSession = repository.createCaptureSession(camera!!, targets)

                // Start preview
                startPreview()


                _cameraState.value = _cameraState.value?.copy(isInitialized = true)
            } catch (e: Exception) {
                _cameraState.value = _cameraState.value?.copy(error = e.message)
                Log.e("CameraViewModel", "Failed to initialize camera", e)
            }
        }
    }

    private fun startPreview() {
        val currentCamera = camera ?: return
        val currentSession = captureSession ?: return
        val surface = previewSurface ?: return
        val settings = _cameraSettings.value ?: CameraSettings()

        viewModelScope.launch {
            try {
                val previewRequest =
                    repository.createPreviewRequest(currentCamera, surface, settings)
                currentSession.setRepeatingRequest(previewRequest, null, null)
            } catch (e: Exception) {
                _cameraState.value = _cameraState.value?.copy(error = e.message)
                Log.e("CameraViewModel", "Failed to start preview", e)
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
        val surface = previewSurface ?: return
        val settings = _cameraSettings.value ?: CameraSettings()

        viewModelScope.launch {
            try {
                val previewRequest = repository.createPreviewRequest(currentCamera, surface, settings)
                currentSession.setRepeatingRequest(previewRequest, null, null)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to update preview", e)
                _cameraState.value = _cameraState.value?.copy(error = e.message)
            }
        }
    }

    fun setPreviewSurface(surface: Surface) {
        previewSurface = surface
    }

    override fun onCleared() {
        super.onCleared()
        camera?.close()
        (repository as? CameraRepositoryImpl)?.cleanup()
    }
}