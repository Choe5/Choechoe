package com.choe.CoreTVLauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.choe.CoreTVLauncher.databinding.ActivityAboutSettingsBinding

class AboutSettingsActivity : FrostedBaseActivity() {

    companion object {
        fun start(context: Context) =
            context.startActivity(Intent(context, AboutSettingsActivity::class.java))
    }

    override val frostedBgViewId: Int get() = R.id.ivFrostedBg

    private lateinit var binding: ActivityAboutSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 顯示版本號
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("1.0.0")
        binding.tvVersion.text = "Version $versionName"

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
