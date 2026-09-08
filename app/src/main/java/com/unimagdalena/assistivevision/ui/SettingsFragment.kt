package com.unimagdalena.assistivevision.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import com.unimagdalena.assistivevision.R
import com.unimagdalena.assistivevision.databinding.FragmentSettingsBinding
import com.unimagdalena.assistivevision.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private var isInitializing = true

    /** Language currently persisted; guards against redundant locale writes. */
    private var currentLanguage = ""

    /** Counts first emissions of every settings collector before user events count. */
    private val initialSyncCount = AtomicInteger(0)

    private fun markInitialSyncDone() {
        if (initialSyncCount.incrementAndGet() >= 5) isInitializing = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBack()
        setupLanguage()
        setupSpeechRate()
        setupConfidence()
        setupMaxObjects()
        setupSwitches()
        observeSettings()
    }

    private fun setupBack() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupLanguage() {
        val languages = listOf("Español" to "es", "English" to "en")
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            languages.map { it.first }
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        binding.languageSpinner.adapter = adapter

        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val lang = languages[pos].second
                if (!isInitializing && lang != currentLanguage) {
                    viewModel.setLanguage(lang)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.create(Locale.forLanguageTag(lang))
                    )
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSpeechRate() {
        // SeekBar 0-20 maps to 0.5x-2.5x (step 0.1)
        binding.speechRateSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rate = 0.5f + progress * 0.1f
                binding.speechRateValue.text = getString(R.string.settings_speech_rate_display, rate)
                if (fromUser) viewModel.setSpeechRate(rate)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setupConfidence() {
        // SeekBar 0-80 maps to 0.10-0.90 (step 0.01)
        binding.confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val confidence = 0.10f + progress * 0.01f
                binding.confidenceValue.text = getString(R.string.settings_confidence_display, confidence)
                if (fromUser) viewModel.setConfidence(confidence)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setupMaxObjects() {
        fun select(max: Int) {
            val selected = listOf(binding.btnMax1, binding.btnMax2, binding.btnMax3)
            selected.forEachIndexed { index, btn ->
                val isSelected = index + 1 == max
                btn.setBackgroundResource(
                    if (isSelected) R.drawable.toggle_selected_bg
                    else R.drawable.toggle_bg
                )
                btn.setTextColor(
                    if (isSelected) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt()
                )
                btn.isSelected = isSelected
            }
            viewModel.setMaxObjects(max)
        }

        binding.btnMax1.setOnClickListener { select(1) }
        binding.btnMax2.setOnClickListener { select(2) }
        binding.btnMax3.setOnClickListener { select(3) }
    }

    private fun setupSwitches() {
        binding.switchShowPreview.setOnCheckedChangeListener { _, checked ->
            if (!isInitializing) viewModel.setShowPreview(checked)
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.language.collect { lang ->
                        currentLanguage = lang
                        val pos = if (lang == "es") 0 else 1
                        binding.languageSpinner.setSelection(pos)
                        markInitialSyncDone()
                    }
                }

                launch {
                    viewModel.speechRate.collect { rate ->
                        val progress = ((rate - 0.5f) / 0.1f).toInt().coerceIn(0, 20)
                        binding.speechRateSeekBar.progress = progress
                        binding.speechRateValue.text = getString(R.string.settings_speech_rate_display, rate)
                        markInitialSyncDone()
                    }
                }

                launch {
                    viewModel.confidence.collect { conf ->
                        val progress = ((conf - 0.10f) / 0.01f).toInt().coerceIn(0, 80)
                        binding.confidenceSeekBar.progress = progress
                        binding.confidenceValue.text = getString(R.string.settings_confidence_display, conf)
                        markInitialSyncDone()
                    }
                }

                launch {
                    viewModel.maxObjects.collect { max ->
                        val btns = listOf(binding.btnMax1, binding.btnMax2, binding.btnMax3)
                        btns.forEachIndexed { index, btn ->
                            val isSelected = index + 1 == max
                            btn.setBackgroundResource(
                                if (isSelected) R.drawable.toggle_selected_bg
                                else R.drawable.toggle_bg
                            )
                            btn.setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt())
                            btn.isSelected = isSelected
                        }
                        markInitialSyncDone()
                    }
                }

                launch {
                    viewModel.showPreview.collect { show ->
                        binding.switchShowPreview.isChecked = show
                        markInitialSyncDone()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}