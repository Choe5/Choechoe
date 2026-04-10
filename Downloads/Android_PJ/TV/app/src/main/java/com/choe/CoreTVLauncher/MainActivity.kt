package com.choe.CoreTVLauncher

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import com.choe.CoreTVLauncher.view.ArcMenuLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.choe.CoreTVLauncher.data.AppRepository
import com.choe.CoreTVLauncher.util.ThumbnailLoader
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.databinding.ActivityMainBinding
import com.choe.CoreTVLauncher.worker.WeatherWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity(), AppRepository.OnAppsChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var appRepository: AppRepository

    private var exoPlayer: ExoPlayer? = null
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateClock()
            timeHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(WeatherWorker.PREF_NAME, MODE_PRIVATE)
        appRepository = (application as ArcFlowApp).appRepository
        appRepository.addListener(this)

        setupArcMenu()
        scheduleWeatherWorker()
        updateWeather()
        binding.arcMenu.post { binding.arcMenu.requestTriggerFocus() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 邏輯 1：如果選單是開啟的，先收起選單
                if (binding.arcMenu.isExpanded) {
                    binding.arcMenu.collapse()
                } else {
                    // 邏輯 2：如果選單已經關閉，這裡不做任何事（即屏蔽返回鍵）
                    // 不呼叫 super.onBackPressed() 或任何跳轉邏輯
                    // 這樣在桌面按下返回鍵就不會有反應
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        timeHandler.post(timeRunnable)
        updateWeather()
        applyWallpaper()
        binding.arcMenu.post { binding.arcMenu.requestTriggerFocus() }
    }

    override fun onPause() {
        super.onPause()
        timeHandler.removeCallbacks(timeRunnable)
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        appRepository.removeListener(this)
    }

    // ─── 弧形選單設定 ────────────────────────────────────────────

    private fun setupArcMenu() {
        // 直接呼叫 buildArcItems 取得帶有顏色與動作的清單
        binding.arcMenu.setItems(buildArcItems())
    }

    private fun buildArcItems(): List<ArcMenuLayout.ArcItem> {
        val arcMenu = binding.arcMenu
        return listOf(
            ArcMenuLayout.ArcItem("▶", "影片", 0xFFFF9800.toInt()) {
                launchApp(prefs.getString("shortcut_video", "") ?: "")
            },
            ArcMenuLayout.ArcItem("♫", "音樂", 0xFFE91E63.toInt()) {
                launchApp(prefs.getString("shortcut_music", "") ?: "")
            },
            ArcMenuLayout.ArcItem("⊞", "全部APP", 0xFF4CAF50.toInt()) {
                AppDrawerActivity.start(this)
            },
            ArcMenuLayout.ArcItem("⚙", "設定", 0xFF607D8B.toInt()) {
                SettingsActivity.start(this)
            },
            ArcMenuLayout.ArcItem("↩", "收起", 0xFFF44336.toInt()) {
                arcMenu.collapse()
            }
        )
    }


    // ─── 桌布套用（圖片 or 影片） ────────────────────────────────

    private fun applyWallpaper() {
        when (val wallpaper = WallpaperManager.getCurrent(this)) {
            is WallpaperItem.Preset -> {
                stopVideoWallpaper()
                binding.ivWallpaper.visibility = View.VISIBLE
                ThumbnailLoader.loadAsset(
                    this,
                    WallpaperManager.assetPath(wallpaper.index),
                    binding.ivWallpaper,
                    targetW = 1920
                )
            }
            is WallpaperItem.Custom -> {
                val uri = Uri.parse(wallpaper.uriString)
                if (wallpaper.isVideo) {
                    binding.ivWallpaper.visibility = View.GONE
                    startVideoWallpaper(uri)
                } else {
                    stopVideoWallpaper()
                    binding.ivWallpaper.visibility = View.VISIBLE
                    ThumbnailLoader.loadUri(
                        this,
                        uri,
                        binding.ivWallpaper,
                        targetW = 1920
                    )
                }
            }
        }
    }

    private fun startVideoWallpaper(uri: Uri) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build().also { player ->
                player.setVideoSurfaceView(binding.surfaceView)
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.volume = 0f
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        binding.ivWallpaper.visibility =
                            if (isPlaying) View.GONE else View.VISIBLE
                    }
                })
            }
        }
        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    private fun stopVideoWallpaper() {
        exoPlayer?.apply {
            stop()
            clearMediaItems()
        }
    }

    // ─── 時鐘更新 ────────────────────────────────────────────────

    private fun updateClock() {
        val now   = Date()
        val is24  = prefs.getBoolean(SettingsActivity.KEY_TIME_FORMAT_24, true)
        val secs  = prefs.getBoolean(SettingsActivity.KEY_SHOW_SECONDS,  false)
        val timeFmt = when {
            is24 && secs  -> "HH:mm:ss"
            is24          -> "HH:mm"
            secs          -> "hh:mm:ss a"
            else          -> "hh:mm a"
        }
        binding.tvTime.text = SimpleDateFormat(timeFmt, Locale.getDefault()).format(now)
        binding.tvDate.text = SimpleDateFormat("M月d日 EEE", Locale.getDefault()).format(now)
    }

    // ─── 天氣顯示 ────────────────────────────────────────────────

    private fun updateWeather() {
        // 天氣顯示開關
        val showWeather = prefs.getBoolean(WeatherWorker.KEY_SHOW_WEATHER, true)
        binding.llWeather.visibility = if (showWeather) View.VISIBLE else View.GONE
        if (!showWeather) return

        val temp = prefs.getInt(WeatherWorker.KEY_WEATHER_TEMP, Int.MIN_VALUE)
        val desc = prefs.getString(WeatherWorker.KEY_WEATHER_DESC, "") ?: ""
        val icon = prefs.getString(WeatherWorker.KEY_WEATHER_ICON, "") ?: ""
        val rawCity = prefs.getString(WeatherWorker.KEY_CITY, WeatherWorker.DEFAULT_CITY) ?: WeatherWorker.DEFAULT_CITY
        val city = rawCity.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }

        binding.tvWeatherCity.text = city
        if (temp != Int.MIN_VALUE) {
            binding.tvWeatherTemp.text = "${temp}°C"
            binding.tvWeatherDesc.text = desc
            binding.tvWeatherIcon.text = weatherIconToEmoji(icon)
        }
    }

    private fun weatherIconToEmoji(icon: String): String = when {
        icon.startsWith("01") -> "☀"
        icon.startsWith("02") -> "⛅"
        icon.startsWith("03") || icon.startsWith("04") -> "☁"
        icon.startsWith("09") || icon.startsWith("10") -> "🌧"
        icon.startsWith("11") -> "⛈"
        icon.startsWith("13") -> "❄"
        icon.startsWith("50") -> "🌫"
        else -> "☁"
    }

    // ─── WorkManager 天氣排程 ────────────────────────────────────

    private fun scheduleWeatherWorker() {
        val request = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeatherWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // ─── App 異動回調 ────────────────────────────────────────────

    override fun onAppsChanged() {
        // 主畫面不需更新
    }

    // ─── 工具函式 ────────────────────────────────────────────────

    private fun launchApp(packageName: String) {
        if (packageName.isBlank()) return
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent)
    }

    private fun showAboutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_about)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = dialog.findViewById<View>(R.id.dialogRoot)
        ObjectAnimator.ofFloat(root, "alpha", 1f, 0.7f, 1f).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        dialog.findViewById<View>(R.id.btnAboutOk).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnAboutOk).requestFocus()
        dialog.show()
    }
}
