package com.example.phonewebcam

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

class CameraStreamManager(
    private val context: Context,
    private val previewView: PreviewView
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val latestJpeg = AtomicReference<ByteArray?>(null)

    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var camera: Camera? = null
    private var currentLensFacing = CameraSelector.LENS_FACING_BACK
    private val currentJpegQuality = AtomicInteger(70)


    fun start(owner: LifecycleOwner) {
        lifecycleOwner = owner
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }
    fun bindCameraUseCases(){
        var provider = cameraProvider ?: return
        var owner = lifecycleOwner ?: return

        var preview = Preview.Builder()
            .build()
            .also{
                it.surfaceProvider = previewView.surfaceProvider
            }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor){ imageProxy->
            try{
                val quality = currentJpegQuality.get()
                val jpeg = imageProxy.toJpeg(quality)
                latestJpeg.set(jpeg)
            }catch(e: Exception){
                e.printStackTrace()
            }finally{
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()

        provider.unbindAll()
        camera = provider.bindToLifecycle(
            owner,
            cameraSelector,
            preview,
            imageAnalysis
        )

    }

    fun switchCamera() {
        currentLensFacing =
            if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

        bindCameraUseCases()
    }

    fun zoomIn() {
        val current = getCurrentZoomRatio()
        val maxZoom = getMaxZoomRatio()
        val newZoom = min(current * 1.2f, maxZoom)
        camera?.cameraControl?.setZoomRatio(newZoom)
    }

    fun zoomOut() {
        val current = getCurrentZoomRatio()
        val minZoom = getMinZoomRatio()
        val newZoom = max(current / 1.2f, minZoom)
        camera?.cameraControl?.setZoomRatio(newZoom)
    }

    fun setZoomRatio(zoomRatio: Float) {
        val minZoom = getMinZoomRatio()
        val maxZoom = getMaxZoomRatio()
        val clamped = zoomRatio.coerceIn(minZoom, maxZoom)
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    fun resetZoom() {
        camera?.cameraControl?.setZoomRatio(1.0f)
    }

    fun getCurrentZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
    }

    fun getMinZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f
    }

    fun getMaxZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1.0f
    }

    fun setJpegQuality(quality: Int) {
        val clamped = quality.coerceIn(10, 95)
        currentJpegQuality.set(clamped)
    }

    fun getJpegQuality(): Int {
        return currentJpegQuality.get()
    }

    fun getCurrentCameraName(): String {
        return if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            "后置"
        } else {
            "前置"
        }
    }

    fun getLatestJpeg(): ByteArray? = latestJpeg.get()

    fun stop() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}

/**
 * 简化版：把 YUV_420_888 转 NV21，再压 JPEG
 */
private fun ImageProxy.toJpeg(quality: Int = 70): ByteArray {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
    return out.toByteArray()
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)

    // 注意：这段在部分机型上可能需要按 rowStride / pixelStride 更严谨处理
    val chroma = ByteArray(vSize + uSize)
    vBuffer.get(chroma, 0, vSize)
    uBuffer.get(chroma, vSize, uSize)

    System.arraycopy(chroma, 0, nv21, ySize, chroma.size)
    return nv21
}