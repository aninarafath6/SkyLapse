//package com.example.android.camera2.basic.fragments
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Color
//import android.graphics.ImageFormat
//import android.hardware.camera2.CameraCaptureSession
//import android.hardware.camera2.CameraCharacteristics
//import android.hardware.camera2.CameraDevice
//import android.hardware.camera2.CameraManager
//import android.hardware.camera2.CaptureRequest
//import android.hardware.camera2.CaptureResult
//import android.hardware.camera2.DngCreator
//import android.hardware.camera2.TotalCaptureResult
//import android.media.Image
//import android.media.ImageReader
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.HandlerThread
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.Surface
//import android.view.SurfaceHolder
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.graphics.drawable.toDrawable
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.Observer
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavController
//import androidx.navigation.Navigation
//import androidx.navigation.fragment.navArgs
//import com.example.android.camera.utils.computeExifOrientation
//import com.example.android.camera.utils.getPreviewOutputSize
//import com.example.android.camera.utils.OrientationLiveData
//import com.example.android.camera2.basic.CameraActivity
//import com.example.android.camera2.basic.R
//import com.example.android.camera2.basic.databinding.FragmentCameraBinding
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.suspendCancellableCoroutine
//import java.io.Closeable
//import java.io.File
//import java.io.FileOutputStream
//import java.io.IOException
//import java.text.SimpleDateFormat
//import java.util.concurrent.ArrayBlockingQueue
//import java.util.concurrent.TimeoutException
//import java.util.Date
//import java.util.Locale
//import kotlin.coroutines.resume
//import kotlin.coroutines.resumeWithException
//import kotlin.coroutines.suspendCoroutine
//
//class CameraFragment : Fragment() {
//
//    private var _fragmentCameraBinding: FragmentCameraBinding? = null
//    private val fragmentCameraBinding get() = _fragmentCameraBinding!!
//    private val args: CameraFragmentArgs by navArgs()
//    private val navController: NavController by lazy {
//        Navigation.findNavController(requireActivity(), R.id.fragment_container)
//    }
//    private val cameraManager: CameraManager by lazy {
//        val context = requireContext().applicationContext
//        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//    }
//
//    private val characteristics: CameraCharacteristics by lazy {
//        cameraManager.getCameraCharacteristics(args.cameraId)
//    }
//    private lateinit var imageReader: ImageReader
//    private val cameraThread = HandlerThread("CameraThread").apply { start() }
//    private val cameraHandler = Handler(cameraThread.looper)
//
//    private val animationTask: Runnable by lazy {
//        Runnable {
//            fragmentCameraBinding.overlay.background = Color.argb(150, 255, 255, 255).toDrawable()
//            fragmentCameraBinding.overlay.postDelayed({
//                fragmentCameraBinding.overlay.background = null
//            }, CameraActivity.ANIMATION_FAST_MILLIS)
//        }
//    }
//
//    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
//    private val imageReaderHandler = Handler(imageReaderThread.looper)
//    private lateinit var camera: CameraDevice
//    private lateinit var session: CameraCaptureSession
//    private lateinit var relativeOrientation: OrientationLiveData
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
//        return fragmentCameraBinding.root
//    }
//
//    @SuppressLint("MissingPermission")
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        fragmentCameraBinding.captureButton.setOnApplyWindowInsetsListener { v, insets ->
//            v.translationX = (-insets.systemWindowInsetRight).toFloat()
//            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
//            insets.consumeSystemWindowInsets()
//        }
//
//        fragmentCameraBinding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
//
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int
//            ) = Unit
//
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                val previewSize = getPreviewOutputSize(
//                    fragmentCameraBinding.viewFinder.display,
//                    characteristics,
//                    SurfaceHolder::class.java
//                )
//
//                fragmentCameraBinding.viewFinder.setAspectRatio(
//                    previewSize.width,
//                    previewSize.height
//                )
//                view.post { initializeCamera() }
//            }
//        })
//
//        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
//            observe(viewLifecycleOwner, Observer { orientation ->
//                Log.d(TAG, "Orientation changed: $orientation")
//            })
//        }
//    }
//
//    /**
//     * Begin all camera operations in a coroutine in the main thread. This function:
//     * - Opens the camera
//     * - Configures the camera session
//     * - Starts the preview by dispatching a repeating capture request
//     * - Sets up the still image capture listeners
//     */
//    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
//        camera = openCamera(cameraManager, args.cameraId, cameraHandler)
//        val size = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
//            .getOutputSizes(args.pixelFormat)
//            .maxByOrNull { it.height * it.width }!!
//
//        imageReader = ImageReader.newInstance(
//            size.width,
//            size.height,
//            args.pixelFormat,
//            IMAGE_BUFFER_SIZE
//        )
//
//        val targets = listOf(fragmentCameraBinding.viewFinder.holder.surface, imageReader.surface)
//        session = createCaptureSession(camera, targets, cameraHandler)
//
//        val captureRequest = camera
//            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//            .apply { addTarget(fragmentCameraBinding.viewFinder.holder.surface) }
//
//        // This will keep sending the capture request as frequently as possible until the
//        // session is torn down or session.stopRepeating() is called
//        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
//
////        fragmentCameraBinding.captureButton.setOnClickListener {
////
////            // Disable click listener to prevent multiple requests simultaneously in flight
////            it.isEnabled = false
////
////            // Perform I/O heavy operations in a different scope
////            lifecycleScope.launch(Dispatchers.IO) {
////                takePhoto().use { result ->
////                    Log.d(TAG, "Result received: $result")
////
////                    // Save the result to disk
////                    val output = saveResult(result)
////                    Log.d(TAG, "Image saved: ${output.absolutePath}")
////
////                    // If the result is a JPEG file, update EXIF metadata with orientation info
////                    if (output.extension == "jpg") {
////                        val exif = ExifInterface(output.absolutePath)
////                        exif.setAttribute(
////                            ExifInterface.TAG_ORIENTATION, result.orientation.toString()
////                        )
////                        exif.saveAttributes()
////                        Log.d(TAG, "EXIF metadata saved: ${output.absolutePath}")
////                    }
////
////                    // Display the photo taken to user
////                    lifecycleScope.launch(Dispatchers.Main) {
////                        navController.navigate(
////                            CameraFragmentDirections
////                                .actionCameraToJpegViewer(output.absolutePath)
////                                .setOrientation(result.orientation)
////                                .setDepth(
////                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
////                                            result.format == ImageFormat.DEPTH_JPEG
////                                )
////                        )
////                    }
////                }
////
////                // Re-enable click listener after photo is taken
////                it.post { it.isEnabled = true }
////    }
////}
//    }
//
//    @SuppressLint("MissingPermission")
//    private suspend fun openCamera(
//        manager: CameraManager,
//        cameraId: String,
//        handler: Handler? = null
//    ): CameraDevice = suspendCancellableCoroutine { cont ->
//        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
//            override fun onOpened(device: CameraDevice) = cont.resume(device)
//
//            override fun onDisconnected(device: CameraDevice) {
//                Log.w(TAG, "Camera $cameraId has been disconnected")
//                requireActivity().finish()
//            }
//
//            override fun onError(device: CameraDevice, error: Int) {
//                val msg = when (error) {
//                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
//                    ERROR_CAMERA_DISABLED -> "Device policy"
//                    ERROR_CAMERA_IN_USE -> "Camera in use"
//                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
//                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
//                    else -> "Unknown"
//                }
//                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
//                Log.e(TAG, exc.message, exc)
//                if (cont.isActive) cont.resumeWithException(exc)
//            }
//        }, handler)
//    }
//
//    private suspend fun createCaptureSession(
//        device: CameraDevice,
//        targets: List<Surface>,
//        handler: Handler? = null
//    ): CameraCaptureSession = suspendCoroutine { cont ->
//
//        // Create a capture session using the predefined targets; this also involves defining the
//        // session state callback to be notified of when the session is ready
//        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
//
//            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
//
//            override fun onConfigureFailed(session: CameraCaptureSession) {
//                val exc = RuntimeException("Camera ${device.id} session configuration failed")
//                Log.e(TAG, exc.message, exc)
//                cont.resumeWithException(exc)
//            }
//        }, handler)
//    }
//
//    private suspend fun takePhoto():
//            CombinedCaptureResult = suspendCoroutine { cont ->
//
//        // Flush any images left in the image reader
//        @Suppress("ControlFlowWithEmptyBody")
//        while (imageReader.acquireNextImage() != null) {
//        }
//
//        // Start a new image queue
//        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
//        imageReader.setOnImageAvailableListener({ reader ->
//            val image = reader.acquireNextImage()
//            Log.d(TAG, "Image available in queue: ${image.timestamp}")
//            imageQueue.add(image)
//        }, imageReaderHandler)
//
//        val captureRequest = session.device.createCaptureRequest(
//            CameraDevice.TEMPLATE_STILL_CAPTURE
//        ).apply { addTarget(imageReader.surface) }
//        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {
//
//            override fun onCaptureStarted(
//                session: CameraCaptureSession,
//                request: CaptureRequest,
//                timestamp: Long,
//                frameNumber: Long
//            ) {
//                super.onCaptureStarted(session, request, timestamp, frameNumber)
//                fragmentCameraBinding.viewFinder.post(animationTask)
//            }
//
//            override fun onCaptureCompleted(
//                session: CameraCaptureSession,
//                request: CaptureRequest,
//                result: TotalCaptureResult
//            ) {
//                super.onCaptureCompleted(session, request, result)
//                val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
//                Log.d(TAG, "Capture result received: $resultTimestamp")
//
//                // Set a timeout in case image captured is dropped from the pipeline
//                val exc = TimeoutException("Image dequeuing took too long")
//                val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
//                imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)
//
//                // Loop in the coroutine's context until an image with matching timestamp comes
//                // We need to launch the coroutine context again because the callback is done in
//                //  the handler provided to the `capture` method, not in our coroutine context
//                @Suppress("BlockingMethodInNonBlockingContext")
//                lifecycleScope.launch(cont.context) {
//                    while (true) {
//
//                        // Dequeue images while timestamps don't match
//                        val image = imageQueue.take()
//                        // TODO(owahltinez): b/142011420
//                        // if (image.timestamp != resultTimestamp) continue
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
//                            image.format != ImageFormat.DEPTH_JPEG &&
//                            image.timestamp != resultTimestamp
//                        ) continue
//                        Log.d(TAG, "Matching image dequeued: ${image.timestamp}")
//
//                        // Unset the image reader listener
//                        imageReaderHandler.removeCallbacks(timeoutRunnable)
//                        imageReader.setOnImageAvailableListener(null, null)
//
//                        // Clear the queue of images, if there are left
//                        while (imageQueue.size > 0) {
//                            imageQueue.take().close()
//                        }
//
//                        // Compute EXIF orientation metadata
//                        val rotation = relativeOrientation.value ?: 0
//                        val mirrored = characteristics.get(CameraCharacteristics.LENS_FACING) ==
//                                CameraCharacteristics.LENS_FACING_FRONT
//                        val exifOrientation = computeExifOrientation(rotation, mirrored)
//
//                        // Build the result and resume progress
//                        cont.resume(
//                            CombinedCaptureResult(
//                                image, result, exifOrientation, imageReader.imageFormat
//                            )
//                        )
//
//                        // There is no need to break out of the loop, this coroutine will suspend
//                    }
//                }
//            }
//        }, cameraHandler)
//    }
//
//    /** Helper function used to save a [CombinedCaptureResult] into a [File] */
//    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
//        when (result.format) {
//
//            // When the format is JPEG or DEPTH JPEG we can simply save the bytes as-is
//            ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
//                val buffer = result.image.planes[0].buffer
//                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
//                try {
//                    val output = createFile(requireContext(), "jpg")
//                    FileOutputStream(output).use { it.write(bytes) }
//                    cont.resume(output)
//                } catch (exc: IOException) {
//                    Log.e(TAG, "Unable to write JPEG image to file", exc)
//                    cont.resumeWithException(exc)
//                }
//            }
//
//            // When the format is RAW we use the DngCreator utility library
//            ImageFormat.RAW_SENSOR -> {
//                val dngCreator = DngCreator(characteristics, result.metadata)
//                try {
//                    val output = createFile(requireContext(), "dng")
//                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
//                    cont.resume(output)
//                } catch (exc: IOException) {
//                    Log.e(TAG, "Unable to write DNG image to file", exc)
//                    cont.resumeWithException(exc)
//                }
//            }
//
//            // No other formats are supported by this sample
//            else -> {
//                val exc = RuntimeException("Unknown image format: ${result.image.format}")
//                Log.e(TAG, exc.message, exc)
//                cont.resumeWithException(exc)
//            }
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        try {
//            camera.close()
//        } catch (exc: Throwable) {
//            Log.e(TAG, "Error closing camera", exc)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraThread.quitSafely()
//        imageReaderThread.quitSafely()
//    }
//
//    override fun onDestroyView() {
//        _fragmentCameraBinding = null
//        super.onDestroyView()
//    }
//
//    companion object {
//        private val TAG = CameraFragment::class.java.simpleName
//
//        /** Maximum number of images that will be held in the reader's buffer */
//        private const val IMAGE_BUFFER_SIZE: Int = 3
//
//        /** Maximum time allowed to wait for the result of an image capture */
//        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000
//
//        data class CombinedCaptureResult(
//            val image: Image,
//            val metadata: CaptureResult,
//            val orientation: Int,
//            val format: Int
//        ) : Closeable {
//            override fun close() = image.close()
//        }
//
//        private fun createFile(context: Context, extension: String): File {
//            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
//            return File(context.filesDir, "IMG_${sdf.format(Date())}.$extension")
//        }
//    }
//}
