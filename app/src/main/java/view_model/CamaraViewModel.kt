package view_model

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlin.collections.contains

class CamaraViewModel : ViewModel() {
    private lateinit var cameraManager: CameraManager

    fun init(context: Context) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val availableCameras = getAvailableGemCam()
        Log.d("CamaraViewModel", "Available Cameras: $availableCameras")
    }


    fun getAvailableGemCam(): String? {
        val cameraIds = cameraManager.cameraIdList.filter { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val capabilities =
                characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true &&
                    capabilities.contains(CameraMetadata.LENS_FACING_BACK)
        }

        cameraIds.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val capabilities =
                characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
            val outputFormats =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

            if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
                && outputFormats.contains(ImageFormat.RAW_SENSOR)
            ) {
                return id
            }
        }

        return cameraIds.firstOrNull()
    }
}