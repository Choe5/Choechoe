package com.choe.CoreTVLauncher

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.worker.WeatherWorker

/**
 * 桌布設定管理器
 *
 * 持久化至 SharedPreferences（與天氣共用 arcflow_prefs）。
 * 預設 assets 路徑：assets/wallpapers/bg0.jpg（預設桌布）
 *                   assets/wallpapers/bg1.jpg ~ bg20.jpg（備選預設圖）
 */
object WallpaperManager {

    const val PRESET_COUNT = 19          // bg1 ~ bg20
    const val DEFAULT_PRESET_INDEX = 1   // bg1 = 預設桌布

    private const val KEY_TYPE = "wallpaper_type"
    private const val KEY_PRESET_INDEX = "wallpaper_preset_index"
    private const val KEY_CUSTOM_URI = "wallpaper_custom_uri"
    private const val KEY_IS_VIDEO = "wallpaper_is_video"

    private const val TYPE_PRESET = "preset"
    private const val TYPE_CUSTOM = "custom"

    // assets 內的路徑前綴
    fun assetFileName(index: Int) = "bg$index.jpg"
    fun assetPath(index: Int) = "wallpapers/bg$index.jpg"

    // Glide 可讀的 assets URI
    fun assetUri(index: Int): Uri =
        Uri.parse("file:///android_asset/wallpapers/bg$index.jpg")

    // ─── 讀取 ─────────────────────────────────────────────────

    fun getCurrent(context: Context): WallpaperItem {
        val prefs = prefs(context)
        return when (prefs.getString(KEY_TYPE, TYPE_PRESET)) {
            TYPE_CUSTOM -> {
                val uri = prefs.getString(KEY_CUSTOM_URI, "") ?: ""
                val isVideo = prefs.getBoolean(KEY_IS_VIDEO, false)
                if (uri.isNotBlank()) WallpaperItem.Custom(uri, isVideo)
                else WallpaperItem.Preset(DEFAULT_PRESET_INDEX)
            }
            else -> WallpaperItem.Preset(
                prefs.getInt(KEY_PRESET_INDEX, DEFAULT_PRESET_INDEX)
            )
        }
    }

    // ─── 寫入 ─────────────────────────────────────────────────

    fun savePreset(context: Context, index: Int) {
        prefs(context).edit()
            .putString(KEY_TYPE, TYPE_PRESET)
            .putInt(KEY_PRESET_INDEX, index)
            .apply()
    }

    fun saveCustom(context: Context, uri: Uri, isVideo: Boolean) {
        prefs(context).edit()
            .putString(KEY_TYPE, TYPE_CUSTOM)
            .putString(KEY_CUSTOM_URI, uri.toString())
            .putBoolean(KEY_IS_VIDEO, isVideo)
            .apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(WeatherWorker.PREF_NAME, Context.MODE_PRIVATE)
}
