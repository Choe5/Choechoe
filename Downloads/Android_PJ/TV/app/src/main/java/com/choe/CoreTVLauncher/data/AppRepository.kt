package com.choe.CoreTVLauncher.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

class AppRepository(private val context: Context) {

    interface OnAppsChangedListener {
        fun onAppsChanged()
    }

    private val _apps = mutableListOf<AppInfo>()
    val apps: List<AppInfo> get() = _apps

    private val listeners = mutableListOf<OnAppsChangedListener>()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun addListener(l: OnAppsChangedListener) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: OnAppsChangedListener) { listeners.remove(l) }

    /**
     * 非同步載入所有可啟動的 APP。
     *
     * 策略：getInstalledApplications → 過濾有 launch intent 的 → 排除自身。
     * 這是最可靠的方式，不依賴特定 LEANBACK/LAUNCHER category。
     */
    fun loadAppsAsync(onDone: () -> Unit) {
        executor.submit {
            val pm = context.packageManager
            val loaded = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo ->
                    // 有 launch intent 且不是自己
                    appInfo.packageName != context.packageName &&
                    pm.getLaunchIntentForPackage(appInfo.packageName) != null
                }
                .mapNotNull { appInfo ->
                    runCatching {
                        AppInfo(
                            packageName = appInfo.packageName,
                            label       = pm.getApplicationLabel(appInfo).toString(),
                            icon        = pm.getApplicationIcon(appInfo.packageName)
                        )
                    }.getOrNull()
                }
                .sortedBy { it.label }

            mainHandler.post {
                _apps.clear()
                _apps.addAll(loaded)
                onDone()
            }
        }
    }

    fun onPackageAdded(packageName: String) {
        if (_apps.none { it.packageName == packageName }) {
            val pm = context.packageManager
            runCatching {
                val info  = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(info).toString()
                val icon  = pm.getApplicationIcon(packageName)
                _apps.add(AppInfo(packageName, label, icon))
                _apps.sortBy { it.label }
            }
        }
        notifyListeners()
    }

    fun onPackageRemoved(packageName: String) {
        _apps.removeAll { it.packageName == packageName }
        notifyListeners()
    }

    fun onPackageReplaced(packageName: String) {
        onPackageRemoved(packageName)
        onPackageAdded(packageName)
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it.onAppsChanged() }
    }
}
