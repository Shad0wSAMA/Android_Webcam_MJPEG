package com.example.phonewebcam

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.text.format.Formatter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.view.ScaleGestureDetector
import android.widget.PopupMenu

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvInfo: TextView

    private lateinit var streamManager: CameraStreamManager

    private lateinit var btnSwitchCamera: Button

    private lateinit var btnZoomIn: Button
    private lateinit var btnZoomOut: Button
    private lateinit var btnResetZoom: Button
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var btnQuality: Button

    private var server: MjpegHttpServer? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraAndServer()
            } else {
                tvInfo.text = "没有相机权限"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        tvInfo = findViewById(R.id.tvInfo)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnZoomIn = findViewById(R.id.btnZoomIn)
        btnZoomOut = findViewById(R.id.btnZoomOut)
        btnResetZoom = findViewById(R.id.btnResetZoom)
        btnQuality = findViewById(R.id.btnQuality)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraAndServer()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraAndServer() {
        streamManager = CameraStreamManager(this, previewView)
        streamManager.start(this)

        server = MjpegHttpServer(8080, streamManager).apply { start() }

        updateInfoText()

        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val currentZoom = streamManager.getCurrentZoomRatio()
                    val newZoom = currentZoom * detector.scaleFactor
                    streamManager.setZoomRatio(newZoom)
                    updateInfoText()
                    return true
                }
            }
        )

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }

        btnSwitchCamera.setOnClickListener {
            streamManager.switchCamera()
            updateInfoText()
        }
        btnZoomIn.setOnClickListener {
            streamManager.zoomIn()
            previewView.postDelayed({ updateInfoText() }, 100)
        }

        btnZoomOut.setOnClickListener {
            streamManager.zoomOut()
            previewView.postDelayed({ updateInfoText() }, 100)
        }

        btnResetZoom.setOnClickListener {
            streamManager.resetZoom()
            previewView.postDelayed({ updateInfoText() }, 100)
        }
        btnQuality.setOnClickListener {
            showQualityMenu()
        }
        updateQualityButtonText()
    }

    private fun updateInfoText() {
        val ip = getLocalIpAddress()
        val cameraName = streamManager.getCurrentCameraName()
        val zoom = String.format("%.1f", streamManager.getCurrentZoomRatio())
        val quality = streamManager.getJpegQuality()

        tvInfo.text = "当前摄像头：$cameraName\n缩放：${zoom}x\nJPEG画质：$quality\n流地址：http://$ip:8080/mjpeg"
    }
    private fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
    }
    private fun showQualityMenu() {
        val popup = PopupMenu(this, btnQuality)

        popup.menu.add(0, 1, 0, "Low (45)")
        popup.menu.add(0, 2, 1, "Medium (60)")
        popup.menu.add(0, 3, 2, "High (80)")
        popup.menu.add(0, 4, 3, "Ultra (90)")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> streamManager.setJpegQuality(45)
                2 -> streamManager.setJpegQuality(60)
                3 -> streamManager.setJpegQuality(80)
                4 -> streamManager.setJpegQuality(90)
            }

            updateInfoText()
            updateQualityButtonText()
            true
        }

        popup.show()
    }

    private fun updateQualityButtonText() {
        btnQuality.text = "Quality: ${streamManager.getJpegQuality()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        if (::streamManager.isInitialized) {
            streamManager.stop()
        }
    }

}