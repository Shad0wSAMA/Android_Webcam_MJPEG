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

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvInfo: TextView

    private lateinit var streamManager: CameraStreamManager

    private lateinit var btnSwitchCamera: Button

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

        btnSwitchCamera.setOnClickListener {
            streamManager.switchCamera()
            updateInfoText()
        }
    }

    private fun updateInfoText() {
        val ip = getLocalIpAddress()
        tvInfo.text = "当前摄像头：${streamManager.getCurrentCameraName()}\n流地址：http://$ip:8080/mjpeg"
    }
    private fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        if (::streamManager.isInitialized) {
            streamManager.stop()
        }
    }
}