package com.choe.CoreTVLauncher

import android.net.Uri
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.util.ThumbnailLoader

/**
 * 帶毛玻璃桌布背景的 Activity 基底類別。
 *
 * 以極低解析度（80px 寬）載入當前桌布，再讓 ImageView centerCrop 放大，
 * 產生自然的模糊／像素化效果，搭配暗色遮罩即形成毛玻璃質感。
 */
abstract class FrostedBaseActivity : AppCompatActivity() {

    /** 子類別覆寫，回傳布局中桌布背景 ImageView 的 id（通常是 R.id.ivFrostedBg） */
    abstract val frostedBgViewId: Int

    override fun onResume() {
        super.onResume()
        val iv = findViewById<ImageView>(frostedBgViewId) ?: return
        loadFrostedBackground(iv)
    }

    private fun loadFrostedBackground(imageView: ImageView) {
        // 以 80px 寬度載入 → 自然模糊效果，幾乎無記憶體消耗
        when (val w = WallpaperManager.getCurrent(this)) {
            is WallpaperItem.Preset ->
                ThumbnailLoader.loadAsset(
                    this,
                    WallpaperManager.assetPath(w.index),
                    imageView,
                    targetW = 80
                )
            is WallpaperItem.Custom -> {
                if (!w.isVideo) {
                    ThumbnailLoader.loadUri(
                        this,
                        Uri.parse(w.uriString),
                        imageView,
                        targetW = 80
                    )
                }
                // 影片桌布：保留 ImageView 空白（暗色遮罩仍可見）
            }
        }
    }
}
