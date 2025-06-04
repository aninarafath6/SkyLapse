package view_model

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.ImageReader
import android.util.Log
import android.view.SurfaceHolder
import androidx.lifecycle.ViewModel
import com.example.android.camera.utils.AutoFitSurfaceView
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.getPreviewOutputSize
import kotlin.collections.contains

class CamaraViewModel : ViewModel() {
    private lateinit var cameraManager: CameraManager
    private lateinit var imageReader: ImageReader
    private lateinit var camera: CameraDevice
    private lateinit var session: CameraCaptureSession
    private lateinit var relativeOrientation: OrientationLiveData
    private lateinit var characteristics: CameraCharacteristics

    fun init(context: Context, viewFinder: AutoFitSurfaceView) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val availableCameras = getAvailableGemCam()
        if (availableCameras != null) {
            characteristics = cameraManager.getCameraCharacteristics(availableCameras)
            setViewFinderHolderListener(viewFinder)
        }
    }


    private fun getAvailableGemCam(): String? {
        val cameraIds = cameraManager.cameraIdList.filter { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val capabilities =
                characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true && capabilities.contains(
                CameraMetadata.LENS_FACING_BACK
            )
        }

        cameraIds.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val capabilities =
                characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
            val outputFormats =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

            if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) && outputFormats.contains(
                    ImageFormat.RAW_SENSOR
                )
            ) {
                return id
            }
        }

        return cameraIds.firstOrNull()
    }


    private fun setViewFinderHolderListener(viewFinder: AutoFitSurfaceView) {

        viewFinder.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                val previewSize = getPreviewOutputSize(
                    viewFinder.display,
                    characteristics,
                    SurfaceHolder::class.java
                )
                Log.d(
                    "CamaraViewModel",
                    "View finder size: ${viewFinder.width} x ${viewFinder.height}"
                )
                Log.d(
                    "CamaraViewModel",
                    "Selected preview size: $previewSize"
                )
                viewFinder.setAspectRatio(previewSize.width, previewSize.height)

                viewFinder.post {
//                    initializeCamera()
                }
            }
        })
    }
}