package views

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.android.camera2.basic.databinding.FragmentCameraBinding
import androidx.navigation.fragment.navArgs
import model.CameraCapabilities
import model.CameraEvent
import model.CameraSettings
import repo.CameraRepositoryImpl
import view_model.CameraViewModel
import view_model.CameraViewModelFactory
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.fragment.app.Fragment
import com.example.android.camera2.basic.R

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val args: CameraFragmentArgs by navArgs()
    private lateinit var viewModel: CameraViewModel

    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
        observeViewModel()
    }

    private fun setupViewModel() {
        val cameraManager =
            requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val repository = CameraRepositoryImpl(requireContext(), cameraManager)
        val factory = CameraViewModelFactory(repository, args.cameraId)
        viewModel = ViewModelProvider(this, factory)[CameraViewModel::class.java]
    }

    private fun setupUI() {
        binding.captureButton.setOnClickListener {
            viewModel.handleEvent(CameraEvent.TakePhoto)
        }

        // Add other UI setup like manual control sliders
        setupManualControls()

        binding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                viewModel.handleEvent(CameraEvent.InitializeCamera)
            }
        })
    }

    private fun setupManualControls() {
        // Add UI controls for manual camera settings
        // Example: Shutter speed slider, ISO slider, etc.
    }

    private fun observeViewModel() {
        viewModel.cameraState.observe(viewLifecycleOwner) { state ->
            binding.captureButton.isEnabled = !state.isCapturing && state.isInitialized

            state.error?.let { error ->
                // Show error to user
                Log.e("CameraFragment", "Camera error: $error")
            }

            state.lastCapturedImagePath?.let { imagePath ->
                // Navigate to image viewer or show success message
                navigateToImageViewer(imagePath)
            }
        }

        viewModel.cameraSettings.observe(viewLifecycleOwner) { settings ->
            // Update UI to reflect current camera settings
            updateManualControlsUI(settings)
        }

        viewModel.cameraCapabilities.observe(viewLifecycleOwner) { capabilities ->
            // Enable/disable manual controls based on capabilities
            updateManualControlsAvailability(capabilities)
        }
    }

    private fun updateManualControlsUI(settings: CameraSettings) {
        // Update sliders/buttons to show current settings
    }

    private fun updateManualControlsAvailability(capabilities: CameraCapabilities) {
        // Enable/disable manual controls based on what the camera supports
    }

    private fun navigateToImageViewer(imagePath: String) {
        // Navigate to image viewer
        navController.navigate(
            CameraFragmentDirections.actionCameraToJpegViewer(imagePath)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
