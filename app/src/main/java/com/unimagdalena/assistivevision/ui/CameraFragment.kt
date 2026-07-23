package com.unimagdalena.assistivevision.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var captureModule: CaptureModule

    private var isFullscreen = false
    private var isPaused = false
    private var isAutomatic = true

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

        viewModel.initialize(requireContext())
        setupCamera()
        setupListeners()
        observeViewModel()
        setMode(true)
    }

    private fun setupCamera() {
        captureModule = CaptureModule(requireContext(), viewLifecycleOwner) { bitmap ->
            if (!isPaused) viewModel.processFrame(bitmap)
        }
        captureModule.start(binding.previewView)
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

        // Mode toggle
        binding.btnAutomatic.setOnClickListener { setMode(automatic = true) }
        binding.btnManual.setOnClickListener { setMode(automatic = false) }

        // Pause / Resume
        binding.btnPause.setOnClickListener {
            isPaused = !isPaused
            binding.pauseIcon.setImageResource(
                if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
            )
            binding.pauseLabel.text = if (isPaused) getString(R.string.action_resume) else getString(R.string.action_pause)
            binding.statusText.text = if (isPaused) getString(R.string.status_text_paused) else getString(R.string.status_text_detecting)
            binding.statusDot.setBackgroundResource(
                if (isPaused) R.drawable.status_dot_inactive
                else R.drawable.status_dot_active
            )
        }

        // Speak (manual mode only)
        binding.btnSpeak.setOnClickListener {
            if (!isAutomatic) viewModel.speakNow()
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_settings)
        }
    }

    private fun setMode(automatic: Boolean) {
        isAutomatic = automatic
        binding.btnAutomatic.apply {
            setBackgroundResource(if (automatic) R.drawable.toggle_selected_bg else android.R.color.transparent)
            backgroundTintList = null
        }
        binding.btnManual.apply {
            setBackgroundResource(if (!automatic) R.drawable.toggle_selected_bg else android.R.color.transparent)
            backgroundTintList = null
        }
        binding.btnAutomatic.setTextColor(
            if (automatic) 0xFFFFFFFF.toInt() else 0x66FFFFFF.toInt()
        )
        binding.btnManual.setTextColor(
            if (!automatic) 0xFFFFFFFF.toInt() else 0x66FFFFFF.toInt()
        )
        binding.btnSpeak.alpha = if (automatic) 0.4f else 1.0f
        binding.btnSpeak.isClickable = !automatic
        viewModel.setAutomatic(automatic)
    }

    private fun observeViewModel() {
        viewModel.detectionCount.observe(viewLifecycleOwner) { count ->
            binding.objectCount.text = getString(R.string.objects_count, count)
        }
        viewModel.detections.observe(viewLifecycleOwner) { detections ->
            binding.overlayView.setDetections(detections)
        }
        viewModel.zoneLabel.observe(viewLifecycleOwner) { label ->
            binding.zoneLabel.text = label
        }
        viewModel.fps.observe(viewLifecycleOwner) { fps ->
            if (!isPaused) binding.statusText.text = getString(R.string.status_fps, fps)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        captureModule.shutdown()
        _binding = null
    }
}