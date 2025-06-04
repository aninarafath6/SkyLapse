package view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import repo.I.CameraRepository

class CameraViewModelFactory(
    private val repository: CameraRepository,
    private val cameraId: String,
    private val pixelFormat: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(repository, cameraId, pixelFormat) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}