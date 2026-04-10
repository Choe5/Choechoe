package com.choe.CoreTVLauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import com.choe.CoreTVLauncher.databinding.ActivityTimeSettingsBinding
import com.choe.CoreTVLauncher.worker.WeatherWorker

class TimeSettingsActivity : FrostedBaseActivity() {

    companion object {
        fun start(context: Context) =
            context.startActivity(Intent(context, TimeSettingsActivity::class.java))
    }

    override val frostedBgViewId: Int get() = R.id.ivFrostedBg

    private lateinit var binding: ActivityTimeSettingsBinding
    private lateinit var prefs: SharedPreferences

    private var is24Hour    = true
    private var showSeconds = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(WeatherWorker.PREF_NAME, MODE_PRIVATE)

        loadPrefs()
        setupButtons()
    }

    private fun loadPrefs() {
        is24Hour    = prefs.getBoolean(SettingsActivity.KEY_TIME_FORMAT_24, true)
        showSeconds = prefs.getBoolean(SettingsActivity.KEY_SHOW_SECONDS,  false)
        updateTimeFormatBtn()
        updateShowSecondsBtn()
    }

    private fun setupButtons() {
        binding.btnTimeFormat.setOnClickListener {
            is24Hour = !is24Hour
            updateTimeFormatBtn()
            prefs.edit().putBoolean(SettingsActivity.KEY_TIME_FORMAT_24, is24Hour).apply()
            Toast.makeText(this, "已切換為 ${if (is24Hour) "24" else "12"} 小時制", Toast.LENGTH_SHORT).show()
        }

        binding.btnShowSeconds.setOnClickListener {
            showSeconds = !showSeconds
            updateShowSecondsBtn()
            prefs.edit().putBoolean(SettingsActivity.KEY_SHOW_SECONDS, showSeconds).apply()
        }
    }

    private fun updateTimeFormatBtn() {
        binding.btnTimeFormat.text = if (is24Hour) "24 小時制" else "12 小時制"
    }

    private fun updateShowSecondsBtn() {
        binding.btnShowSeconds.text = if (showSeconds) "開啟" else "關閉"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
