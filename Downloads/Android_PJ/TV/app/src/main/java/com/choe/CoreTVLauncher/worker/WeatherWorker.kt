package com.choe.CoreTVLauncher.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * WorkManager 定時天氣任務（每 30 分鐘執行一次）
 *
 * 使用 OpenWeatherMap API：
 * https://api.openweathermap.org/data/2.5/weather?q={CITY}&appid={API_KEY}&units=metric
 *
 * 請在 local.properties 或 SharedPreferences 設定 API Key 與城市名稱。
 */
class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "weather_update"
        const val PREF_NAME = "arcflow_prefs"
        const val KEY_WEATHER_TEMP = "weather_temp"
        const val KEY_WEATHER_DESC = "weather_desc"
        const val KEY_WEATHER_ICON = "weather_icon"
        const val KEY_API_KEY = "weather_api_key"
        const val KEY_CITY = "weather_city"

        // 桌面天氣顯示開關（預設開啟）
        const val KEY_SHOW_WEATHER = "show_weather"

        // 預設值（使用者可在設定頁修改）
        const val DEFAULT_CITY = "Taipei"
    }

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val city = prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY

        if (apiKey.isBlank()) return@withContext Result.success() // 未設定 Key，靜默略過

        runCatching {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?q=${city}&appid=${apiKey}&units=metric&lang=zh_tw"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@runCatching

            val body = response.body?.string() ?: return@runCatching
            val json = JSONObject(body)
            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val desc = json.getJSONArray("weather").getJSONObject(0).getString("description")
            val icon = json.getJSONArray("weather").getJSONObject(0).getString("icon")

            prefs.edit()
                .putInt(KEY_WEATHER_TEMP, temp)
                .putString(KEY_WEATHER_DESC, desc)
                .putString(KEY_WEATHER_ICON, icon)
                .apply()
        }

        Result.success()
    }
}
