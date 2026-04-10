package com.choe.CoreTVLauncher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.choe.CoreTVLauncher.adapter.WallpaperAdapter
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.databinding.ActivityFilePickerBinding

class FilePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 先調整視窗大小與屬性
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * 0.8).toInt()
        val height = (metrics.heightPixels * 0.8).toInt()
        window?.setLayout(width, height)

        // 💡 關鍵：確保背景是透明或符合主題，有時視窗背景會遮擋內容
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 2. 延遲一小段時間或確保 Layout 完成後再載入數據
        // 這樣可以避免 RecyclerView 在視窗大小尚未穩定時就計算佈局
        binding.root.post {
            setupFileGrid()
        }
    }

    // FilePickerActivity.kt 內的 setupFileGrid
    private fun setupFileGrid() {
        val mediaList = fetchAllMedia()

        if (mediaList.isEmpty()) {
            Toast.makeText(this, "找不到媒體檔案", Toast.LENGTH_SHORT).show()
        }

        val defaultSelection: WallpaperItem = mediaList.firstOrNull() ?: WallpaperItem.Preset(1)

        val adapter = WallpaperAdapter(this, mediaList, defaultSelection) { selected: WallpaperItem ->
            if (selected is WallpaperItem.Custom) {
                val resultIntent = Intent().apply {
                    putExtra("selected_uri", selected.uriString)
                    putExtra("is_video", selected.isVideo)
                    putExtra("selected_name", selected.name) // 這裡你已經寫對了
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        binding.rvFileGrid.apply {
            layoutManager = GridLayoutManager(this@FilePickerActivity, 5)
            this.adapter = adapter
            // 💡 修正：開啟後主動要求焦點，解決「無反應」問題
            requestFocus()
        }
    }

    // 這是剛才報錯找不到的函式 fetchAllMedia
    private fun fetchAllMedia(): List<WallpaperItem.Custom> {
        val list = mutableListOf<WallpaperItem.Custom>()
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME
        )

        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}",
            null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )

        cursor?.use {
            val dataIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val mimeIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (it.moveToNext()) {
                val path = it.getString(dataIdx) ?: continue
                val mime = it.getString(mimeIdx) ?: ""
                val name = it.getString(nameIdx) ?: "Unknown"
                list.add(WallpaperItem.Custom(
                    Uri.fromFile(java.io.File(path)).toString(),
                    mime.startsWith("video/"),
                    name = name
                ))
            }
        }
        return list
    }
}