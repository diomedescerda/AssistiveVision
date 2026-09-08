package com.unimagdalena.assistivevision.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.unimagdalena.assistivevision.data.SettingsManager
import com.unimagdalena.assistivevision.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySavedLocale()
    }

    /**
     * Applies the persisted language asynchronously: the first frame renders
     * with the current locale and the activity recreates itself if the saved
     * locale differs. Avoids blocking disk I/O on the main thread.
     */
    private fun applySavedLocale() {
        lifecycleScope.launch {
            val lang = SettingsManager(applicationContext).language.first()
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != lang) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.create(Locale.forLanguageTag(lang))
                )
            }
        }
    }
}
