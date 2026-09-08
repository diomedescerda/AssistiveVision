package com.unimagdalena.assistivevision.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else onPermissionDenied()
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
        val module = CaptureModule(requireContext(), viewLifecycleOwner) { bitmap ->
            viewModel.processFrame(bitmap)
        }
        captureModule = module
        module.start(binding.previewView)
    }

    private fun onPermissionDenied() {
        binding.statusText.text = getString(R.string.camera_permission_denied)
        binding.btnCapture.isEnabled = false
        binding.btnCapture.alpha = 0.5f
    }

    private fun setupListeners() {

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

        // Take photo (runs detection on the latest frame)
        binding.btnCapture.setOnClickListener {
            viewModel.captureFrame()
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
