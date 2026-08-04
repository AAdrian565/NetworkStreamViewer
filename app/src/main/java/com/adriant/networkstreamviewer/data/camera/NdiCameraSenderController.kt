package com.adriant.networkstreamviewer.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.util.Range
import android.view.Surface
import com.adriant.networkstreamviewer.data.ndi.NdiNative
import com.adriant.networkstreamviewer.domain.model.CameraLens
import com.adriant.networkstreamviewer.domain.model.CameraSenderSettings
import kotlin.math.abs

class NdiCameraSenderController(
    context: Context,
    private val onCameraReadyChanged: (Boolean) -> Unit,
    private val onCameraConfigured: (Int, Int, Int) -> Unit,
    private val onSenderProgress: (Long, Int) -> Unit,
    private val onStreamingStopped: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null
    private var generation = 0
    private val nv12Converter = Yuv420ToNv12Converter()
    private var sentFrameCount = 0L

    @Volatile
    private var activeFrameRate = DEFAULT_FRAME_RATE

    @Volatile
    private var cameraReady = false

    @Volatile
    private var streaming = false

    @SuppressLint("MissingPermission")
    fun startCamera(
        surfaceTexture: SurfaceTexture,
        settings: CameraSenderSettings
    ) {
        stopCamera()
        val currentGeneration = ++generation
        try {
            val cameraId = chooseCameraId(settings.lens)
            val frameSize = chooseFrameSize(cameraId, settings.resolution.width, settings.resolution.height)
            val frameRateRange = chooseFrameRateRange(cameraId, settings.frameRate)
            activeFrameRate = settings.frameRate
            surfaceTexture.setDefaultBufferSize(frameSize.width, frameSize.height)
            previewSurface = Surface(surfaceTexture)

            val thread = HandlerThread("ndi-camera").also { it.start() }
            val handler = Handler(thread.looper)
            cameraThread = thread
            cameraHandler = handler
            imageReader = ImageReader.newInstance(
                frameSize.width,
                frameSize.height,
                ImageFormat.YUV_420_888,
                IMAGE_QUEUE_SIZE
            ).also { reader ->
                reader.setOnImageAvailableListener(::onImageAvailable, handler)
            }

            cameraManager.openCamera(
                cameraId,
                cameraStateCallback(currentGeneration, frameRateRange),
                handler
            )
            mainHandler.post {
                onCameraConfigured(frameSize.width, frameSize.height, settings.frameRate)
            }
        } catch (_: SecurityException) {
            notifyError("Camera permission is required.")
            stopCamera()
        } catch (_: Exception) {
            notifyError("The camera could not be opened.")
            stopCamera()
        }
    }

    fun startStreaming(streamName: String): Boolean {
        if (!cameraReady || streamName.isBlank()) return false
        if (!NdiNative.initialize() || !NdiNative.startSender(streamName)) {
            notifyError("The NDI® camera stream could not be started.")
            return false
        }
        sentFrameCount = 0
        streaming = true
        return true
    }

    fun stopStreaming() {
        streaming = false
        NdiNative.stopSender()
    }

    fun stopCamera() {
        generation++
        val wasStreaming = streaming
        stopStreaming()
        if (wasStreaming) mainHandler.post(onStreamingStopped)
        setCameraReady(false)
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        previewSurface?.release()
        previewSurface = null
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }

    private fun cameraStateCallback(
        currentGeneration: Int,
        frameRateRange: Range<Int>?
    ) = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            if (currentGeneration != generation) {
                device.close()
                return
            }
            cameraDevice = device
            createCaptureSession(currentGeneration, device, frameRateRange)
        }

        override fun onDisconnected(device: CameraDevice) {
            device.close()
            if (currentGeneration == generation) {
                notifyError("The camera was disconnected.")
                stopCamera()
            }
        }

        override fun onError(device: CameraDevice, error: Int) {
            device.close()
            if (currentGeneration == generation) {
                notifyError("The camera reported an error ($error).")
                stopCamera()
            }
        }
    }

    @Suppress("DEPRECATION") // Required for Camera2 compatibility on API 26 and 27.
    private fun createCaptureSession(
        currentGeneration: Int,
        device: CameraDevice,
        frameRateRange: Range<Int>?
    ) {
        val reader = imageReader ?: return
        val preview = previewSurface ?: return
        val handler = cameraHandler ?: return
        val surfaces = listOf(preview, reader.surface)
        device.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (currentGeneration != generation) {
                        session.close()
                        return
                    }
                    captureSession = session
                    try {
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            surfaces.forEach(::addTarget)
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                            )
                            frameRateRange?.let {
                                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it)
                            }
                        }.build()
                        session.setRepeatingRequest(request, null, handler)
                        setCameraReady(true)
                    } catch (_: Exception) {
                        notifyError("The camera preview could not be started.")
                        stopCamera()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    session.close()
                    if (currentGeneration == generation) {
                        notifyError("The camera output could not be configured.")
                        stopCamera()
                    }
                }
            },
            handler
        )
    }

    private fun onImageAvailable(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (_: IllegalStateException) {
            null
        } ?: return

        try {
            if (!streaming) return
            val frame = nv12Converter.convert(image)
            if (!NdiNative.sendVideoFrame(frame.data, frame.width, frame.height, activeFrameRate)) {
                streaming = false
                NdiNative.stopSender()
                mainHandler.post(onStreamingStopped)
                notifyError("Sending the camera frame failed.")
            } else {
                sentFrameCount++
                if (sentFrameCount == 1L || sentFrameCount % PROGRESS_FRAME_INTERVAL == 0L) {
                    val connections = NdiNative.senderConnectionCount()
                    val currentFrameCount = sentFrameCount
                    mainHandler.post { onSenderProgress(currentFrameCount, connections) }
                }
            }
        } catch (_: Exception) {
            streaming = false
            NdiNative.stopSender()
            mainHandler.post(onStreamingStopped)
            notifyError("The camera frame could not be converted for NDI®.")
        } finally {
            image.close()
        }
    }

    private fun chooseCameraId(lens: CameraLens): String {
        val ids = cameraManager.cameraIdList
        val requestedFacing = when (lens) {
            CameraLens.BACK -> CameraCharacteristics.LENS_FACING_BACK
            CameraLens.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
        }
        return ids.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == requestedFacing
        } ?: error("The selected camera is not available")
    }

    private fun chooseFrameSize(cameraId: String, targetWidth: Int, targetHeight: Int): Size {
        val map = cameraManager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: error("The camera has no output configuration")
        val sizes = map.getOutputSizes(ImageFormat.YUV_420_888)
            .orEmpty()
            .filter { it.width % 2 == 0 && it.height % 2 == 0 }
        val targetAspectRatio = targetWidth.toFloat() / targetHeight
        val targetArea = targetWidth.toLong() * targetHeight
        return sizes.minWithOrNull(
            compareBy<Size> { abs(it.width.toFloat() / it.height - targetAspectRatio) }
                .thenBy { abs(it.width.toLong() * it.height - targetArea) }
        ) ?: error("The camera has no YUV output size")
    }

    private fun chooseFrameRateRange(cameraId: String, targetFrameRate: Int): Range<Int>? {
        val ranges = cameraManager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            .orEmpty()
        return ranges.filter { targetFrameRate in it }
            .minWithOrNull(
                compareBy<Range<Int>> { it.upper - it.lower }
                    .thenBy { abs(it.upper - targetFrameRate) }
            )
    }

    private fun setCameraReady(ready: Boolean) {
        if (cameraReady == ready) return
        cameraReady = ready
        mainHandler.post { onCameraReadyChanged(ready) }
    }

    private fun notifyError(message: String) {
        mainHandler.post { onError(message) }
    }

    private companion object {
        const val DEFAULT_FRAME_RATE = 30
        const val PROGRESS_FRAME_INTERVAL = 30
        const val IMAGE_QUEUE_SIZE = 3
    }
}
