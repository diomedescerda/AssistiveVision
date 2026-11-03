package com.unimagdalena.assistivevision.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.unimagdalena.assistivevision.R
import com.unimagdalena.assistivevision.databinding.FragmentCameraBinding
import com.unimagdalena.assistivevision.modules.CaptureModule
import com.unimagdalena.assistivevision.viewmodel.MainViewModel

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var captureModule: CaptureModule? = null

    private var isFullscreen = false
    private var isCapturing = false
    private var permissionDenied = false

    /** Frozen bitmap currently shown; recycled once replaced or cleared. */
    private var displayedFrozen: Bitmap? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            startCamera()
        } else onPermissionDenied()
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

        if (isCameraPermissionGranted()) startCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        setupListeners()
        observeViewModel()
        observeSettings()
    }

    private fun isCameraPermissionGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val module = CaptureModule(requireContext(), viewLifecycleOwner)
        captureModule = module
        module.start(binding.previewView)
    }

    /**
     * Captures a JPEG still from the active (0.5x) camera, then runs the
     * detection pipeline on it. Used by the on-screen button and the
     * Bluetooth shutter.
     */
    fun capture() {
        if (isCapturing || viewModel.frozenFrame.value != null) return
        captureModule?.captureStill { bitmap ->
            viewModel.processFrame(bitmap)
            viewModel.captureFrame()
        }
    }

    private fun onPermissionDenied() {
        permissionDenied = true
        updateCaptureButton()
        binding.statusText.text = getString(R.string.camera_permission_denied)
    }

    /** Enables the capture button only when a capture is possible right now. */
    private fun updateCaptureButton() {
        val enabled = !permissionDenied && !isCapturing
        binding.btnCapture.isEnabled = enabled
        binding.btnCapture.alpha = if (enabled) 1f else 0.5f
    }

    /** The container buttons are LinearLayouts; give them a button role for TalkBack. */
    private fun applyButtonRole(vararg views: View) {
        views.forEach { view ->
            view.accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: android.view.accessibility.AccessibilityNodeInfo
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = Button::class.java.name
                }
            }
        }
    }

    private fun setupListeners() {
        applyButtonRole(binding.btnCapture, binding.btnSettings)
        updateCaptureButton()

        // Fullscreen toggle
        binding.btnFullscreen.setOnClickListener {
            isFullscreen = !isFullscreen
            val visibility = if (isFullscreen) View.GONE else View.VISIBLE
            binding.header.visibility = visibility
            binding.bottomPanel.visibility = visibility
            binding.btnFullscreen.setImageResource(
                if (isFullscreen) R.drawable.ic_fullscreen_exit
                else R.drawable.ic_fullscreen
            )
        }

        // Capture a still and run detection on it
        binding.btnCapture.setOnClickListener {
            capture()
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_settings)
        }
    }

    private fun observeViewModel() {
        viewModel.detectionCount.observe(viewLifecycleOwner) { count ->
            binding.objectCount.text = getString(R.string.objects_count, count)
        }
        viewModel.detections.observe(viewLifecycleOwner) { detections ->
            binding.overlayView.setDetections(detections)
        }
        viewModel.frameSize.observe(viewLifecycleOwner) { (width, height) ->
            binding.overlayView.setFrameSize(width, height)
        }
        viewModel.zoneLabel.observe(viewLifecycleOwner) { label ->
            binding.zoneLabel.text = label
        }
        viewModel.isCapturing.observe(viewLifecycleOwner) { capturing ->
            isCapturing = capturing
            updateCaptureButton()
        }
        viewModel.speechUnavailable.observe(viewLifecycleOwner) { unavailable ->
            if (unavailable) binding.statusText.text = getString(R.string.tts_unavailable)
        }
        viewModel.frozenFrame.observe(viewLifecycleOwner) { frozen ->
            binding.statusText.text = if (frozen != null) getString(R.string.status_text_detecting)
            else getString(R.string.status_ready)
            if (frozen != null) {
                binding.capturedImage.setImageBitmap(frozen)
                binding.capturedImage.visibility = View.VISIBLE
            } else {
                binding.capturedImage.setImageDrawable(null)
                binding.capturedImage.visibility = View.GONE
            }
            // This observer runs on the main thread, so once the ImageView no
            // longer references the previous bitmap it can be recycled safely.
            if (displayedFrozen != null && displayedFrozen !== frozen) {
                displayedFrozen?.recycle()
            }
            displayedFrozen = frozen
        }
    }

    private fun observeSettings() {
        viewModel.showPreview.observe(viewLifecycleOwner) { show ->
            binding.previewView.visibility = if (show) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        captureModule?.shutdown()
        captureModule = null
        _binding = null
    }
}
