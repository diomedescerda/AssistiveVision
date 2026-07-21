package com.unimagdalena.assistivevision.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unimagdalena.assistivevision.R
import com.unimagdalena.assistivevision.databinding.ActivityMainBinding
import com.unimagdalena.assistivevision.modules.CaptureModule
import com.unimagdalena.assistivevision.viewmodel.DetectionMode
import com.unimagdalena.assistivevision.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var captureModule: CaptureModule? = null
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.initialize(this)

        setupModeToggle()
        setupCaptureButton()
        observeDetections()

        if(allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }

    private fun setupModeToggle() {
        binding.modeToggle.setOnClickListener {
            val newMode = if (viewModel.mode.value == DetectionMode.AUTO)
                DetectionMode.MANUAL else DetectionMode.AUTO
            viewModel.setMode(newMode)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mode.collect { mode ->
                    binding.modeToggle.setImageResource(
                        if (mode == DetectionMode.AUTO) R.drawable.ic_automatic
                        else R.drawable.ic_manual
                    )
                    binding.captureButton.visibility =
                        if (mode == DetectionMode.MANUAL) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupCaptureButton() {
        binding.captureButton.setOnClickListener {
            viewModel.captureManualFrame()
        }
    }

    private fun observeDetections() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detections.collect { detections ->
                    Log.d("MainActivity", "Detections: ${detections.size}")
                    detections.forEach {
                        Log.d("MainActivity", "  ${it.className} (${it.score}) [${it.zone}]")
                    }
                    binding.overlayView.setDetections(detections)
                }
            }
        }
    }

    private fun startCamera() {
        captureModule = CaptureModule(this, this) { bitmap ->
            viewModel.processFrame(bitmap)
        }
        captureModule?.start(binding.previewView)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureModule?.shutdown()
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
