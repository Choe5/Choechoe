package com.choe.CoreTVLauncher.data

/**
 * 桌布項目資料模型
 *
 * @param Preset   預設圖片（assets/wallpapers/bg0.jpg ~ bg20.jpg）
 * @param Custom   使用者自選圖片或影片（SAF URI）
 */
sealed class WallpaperItem {
    /** index 0 = 預設 bg0.jpg，1~20 = bg1.jpg ~ bg20.jpg */
    data class Preset(val index: Int) : WallpaperItem()

    data class Custom(
        val uriString: String,
        val isVideo: Boolean,
        val name: String = ""
    ) : WallpaperItem()
}
