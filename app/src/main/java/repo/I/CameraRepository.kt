package repo.I

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import model.CameraCapabilities
import model.CameraSettings
import model.CombinedCaptureResult
import java.io.File

interface CameraRepository {
    suspend fun openCamera(cameraId: String): CameraDevice
    suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>
    ): CameraCaptureSession

    suspend fun captureStillPicture(
        session: CameraCaptureSession,
        request: CaptureRequest
    ): CombinedCaptureResult

    suspend fun saveImage(result: CombinedCaptureResult): File

    fun getCameraCapabilities(cameraId: String): CameraCapabilities

    fun createPreviewRequest(
        camera: CameraDevice,
        surface: Surface,
        settings: CameraSettings
    ): CaptureRequest

    fun createCaptureRequest(
        camera: CameraDevice,
        surface: Surface,
        settings: CameraSettings
    ): CaptureRequest

    fun getCameraManager(): CameraManager

}