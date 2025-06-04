package view_model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\n\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0002J\u0016\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016J\u0010\u0010\u0017\u001a\u00020\u00122\u0006\u0010\u0015\u001a\u00020\u0016H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lview_model/CamaraViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "camera", "Landroid/hardware/camera2/CameraDevice;", "cameraManager", "Landroid/hardware/camera2/CameraManager;", "characteristics", "Landroid/hardware/camera2/CameraCharacteristics;", "imageReader", "Landroid/media/ImageReader;", "relativeOrientation", "Lcom/example/android/camera/utils/OrientationLiveData;", "session", "Landroid/hardware/camera2/CameraCaptureSession;", "getAvailableGemCam", "", "init", "", "context", "Landroid/content/Context;", "viewFinder", "Lcom/example/android/camera/utils/AutoFitSurfaceView;", "setViewFinderHolderListener", "app_debug"})
public final class CamaraViewModel extends androidx.lifecycle.ViewModel {
    private android.hardware.camera2.CameraManager cameraManager;
    private android.media.ImageReader imageReader;
    private android.hardware.camera2.CameraDevice camera;
    private android.hardware.camera2.CameraCaptureSession session;
    private com.example.android.camera.utils.OrientationLiveData relativeOrientation;
    private android.hardware.camera2.CameraCharacteristics characteristics;
    
    public CamaraViewModel() {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.example.android.camera.utils.AutoFitSurfaceView viewFinder) {
    }
    
    private final java.lang.String getAvailableGemCam() {
        return null;
    }
    
    private final void setViewFinderHolderListener(com.example.android.camera.utils.AutoFitSurfaceView viewFinder) {
    }
}