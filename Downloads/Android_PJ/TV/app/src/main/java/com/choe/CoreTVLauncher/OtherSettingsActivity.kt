package com.choe.CoreTVLauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.choe.CoreTVLauncher.databinding.ActivityOtherSettingsBinding

class OtherSettingsActivity : FrostedBaseActivity() {

    companion object {
        fun start(context: Context) =
            context.startActivity(Intent(context, OtherSettingsActivity::class.java))
    }

    override val frostedBgViewId: Int get() = R.id.ivFrostedBg

    private lateinit var binding: ActivityOtherSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtherSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
