package com.choe.CoreTVLauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

/**
 * 輕量非同步縮圖載入器（無第三方依賴）
 *
 * 機制：LruCache 快取 + 固定執行緒池背景解碼 + Handler 回主執行緒
 */
object ThumbnailLoader {

    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    // LruCache：最多佔用 1/8 可用 heap
    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /**
     * 從 assets 非同步載入縮圖
     * @param assetPath  e.g. "wallpapers/bg1.jpg"
     * @param targetW    目標寬度（px），用於計算 inSampleSize
     */
    fun loadAsset(
        context: Context,
        assetPath: String,
        imageView: ImageView,
        targetW: Int = 400
    ) {
        val key = "asset:$assetPath"
        cache.get(key)?.let { imageView.setImageBitmap(it); return }

        imageView.tag = key
        executor.submit {
            val bitmap = runCatching {
                context.assets.open(assetPath).use { stream ->
                    decodeSampled(stream, targetW)
                }
            }.getOrNull()

            mainHandler.post {
                if (imageView.tag == key && bitmap != null) {
                    cache.put(key, bitmap)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    /**
     * 從 ContentResolver URI 非同步載入縮圖
     */
    fun loadUri(
        context: Context,
        uri: Uri,
        imageView: ImageView,
        targetW: Int = 400
    ) {
        val key = "uri:${uri}"
        cache.get(key)?.let { imageView.setImageBitmap(it); return }

        imageView.tag = key
        executor.submit {
            val bitmap = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    decodeSampled(stream, targetW)
                }
            }.getOrNull()

            mainHandler.post {
                if (imageView.tag == key && bitmap != null) {
                    cache.put(key, bitmap)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    fun clearCache() = cache.evictAll()

    // ── BitmapFactory 兩段式解碼，計算最佳 inSampleSize ──────────
    private fun decodeSampled(stream: java.io.InputStream, targetW: Int): Bitmap? {
        // 先把 stream 讀成 ByteArray 才能二次解碼
        val bytes = stream.readBytes()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        opts.inSampleSize = calcSampleSize(opts.outWidth, targetW)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun calcSampleSize(srcW: Int, dstW: Int): Int {
        var sample = 1
        if (srcW > dstW) {
            while (srcW / (sample * 2) >= dstW) sample *= 2
        }
        return sample
    }
}
