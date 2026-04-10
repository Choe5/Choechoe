package com.choe.CoreTVLauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.choe.CoreTVLauncher.ArcFlowApp

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val repo = (context.applicationContext as ArcFlowApp).appRepository

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> repo.onPackageAdded(packageName)
            Intent.ACTION_PACKAGE_REMOVED -> repo.onPackageRemoved(packageName)
            Intent.ACTION_PACKAGE_REPLACED -> repo.onPackageReplaced(packageName)
        }
    }
}
