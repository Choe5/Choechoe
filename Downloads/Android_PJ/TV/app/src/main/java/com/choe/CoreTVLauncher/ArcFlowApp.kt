package com.choe.CoreTVLauncher

import android.app.Application
import com.choe.CoreTVLauncher.data.AppRepository

class ArcFlowApp : Application() {
    // AppDrawerActivity 打開時才非同步載入，不在 Application.onCreate() 阻塞
    val appRepository: AppRepository by lazy { AppRepository(this) }
}
