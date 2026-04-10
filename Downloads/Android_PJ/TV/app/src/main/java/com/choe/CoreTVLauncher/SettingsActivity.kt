package com.choe.CoreTVLauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.choe.CoreTVLauncher.databinding.ActivitySettingsBinding

/**
 * 設定樞紐頁面
 *
 * 顯示 5 個彩色圓形圖示，焦點時才顯示文字標籤；
 * 各自開啟對應的子頁面。
 */
class SettingsActivity : FrostedBaseActivity() {

    companion object {
        const val KEY_TIME_FORMAT_24 = "time_format_24h"
        const val KEY_SHOW_SECONDS   = "time_show_seconds"

        fun start(context: Context) =
            context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    override val frostedBgViewId: Int get() = R.id.ivFrostedBg

    private lateinit var binding: ActivitySettingsBinding

    // 圓形 FrameLayout → 對應 Label TextView
    private val circleToLabel: List<Pair<View, View>> by lazy {
        listOf(
            binding.circleTime     to binding.labelTime,
            binding.circleWeather  to binding.labelWeather,
            binding.circleWallpaper to binding.labelWallpaper,
            binding.circleOther    to binding.labelOther,
            binding.circleAbout    to binding.labelAbout
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCircles()

        // 預設焦點給第一個圓
        binding.circleTime.post { binding.circleTime.requestFocus() }
    }

    private fun setupCircles() {
        // 焦點變化：顯示/隱藏標籤 + 縮放動畫
        circleToLabel.forEach { (circle, label) ->
            circle.setOnFocusChangeListener { v, hasFocus ->
                label.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
                v.animate()
                    .scaleX(if (hasFocus) 1.15f else 1f)
                    .scaleY(if (hasFocus) 1.15f else 1f)
                    .setDuration(150)
                    .start()
                if (hasFocus) {
                    v.elevation = 12f * resources.displayMetrics.density
                } else {
                    v.elevation = 4f * resources.displayMetrics.density
                }
            }
        }

        binding.circleTime.setOnClickListener {
            TimeSettingsActivity.start(this)
        }
        binding.circleWeather.setOnClickListener {
            WeatherSettingsActivity.start(this)
        }
        binding.circleWallpaper.setOnClickListener {
            WallpaperPickerActivity.start(this)
        }
        binding.circleOther.setOnClickListener {
            OtherSettingsActivity.start(this)
        }
        binding.circleAbout.setOnClickListener {
            AboutSettingsActivity.start(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
