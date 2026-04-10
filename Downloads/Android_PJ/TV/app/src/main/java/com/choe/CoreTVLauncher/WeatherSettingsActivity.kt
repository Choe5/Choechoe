package com.choe.CoreTVLauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.choe.CoreTVLauncher.databinding.ActivityWeatherSettingsBinding
import com.choe.CoreTVLauncher.worker.WeatherWorker

class WeatherSettingsActivity : FrostedBaseActivity() {

    companion object {
        fun start(context: Context) =
            context.startActivity(Intent(context, WeatherSettingsActivity::class.java))
    }

    override val frostedBgViewId: Int get() = R.id.ivFrostedBg

    private lateinit var binding: ActivityWeatherSettingsBinding
    private lateinit var prefs: SharedPreferences

    private var showWeather = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(WeatherWorker.PREF_NAME, MODE_PRIVATE)

        loadPrefs()
        setupButtons()
    }

    private fun loadPrefs() {
        showWeather = prefs.getBoolean(WeatherWorker.KEY_SHOW_WEATHER, true)
        updateShowWeatherBtn()

        binding.etCity.setText(
            prefs.getString(WeatherWorker.KEY_CITY, WeatherWorker.DEFAULT_CITY)
        )
        binding.etApiKey.setText(
            prefs.getString(WeatherWorker.KEY_API_KEY, "")
        )
    }

    private fun setupButtons() {
        binding.btnShowWeather.setOnClickListener {
            showWeather = !showWeather
            updateShowWeatherBtn()
            prefs.edit().putBoolean(WeatherWorker.KEY_SHOW_WEATHER, showWeather).apply()
        }

        binding.btnSaveWeather.setOnClickListener {
            val city   = binding.etCity.text.toString().trim()
            val apiKey = binding.etApiKey.text.toString().trim()

            if (city.isEmpty()) {
                Toast.makeText(this, "請輸入城市名稱", Toast.LENGTH_SHORT).show()
                binding.etCity.requestFocus()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(WeatherWorker.KEY_CITY,    city)
                .putString(WeatherWorker.KEY_API_KEY, apiKey)
                .apply()

            // 立即觸發一次天氣同步
            WorkManager.getInstance(this)
                .enqueue(OneTimeWorkRequestBuilder<WeatherWorker>().build())

            Toast.makeText(this, "天氣設定已儲存，正在同步天氣…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateShowWeatherBtn() {
        binding.btnShowWeather.text = if (showWeather) "開啟" else "關閉"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
