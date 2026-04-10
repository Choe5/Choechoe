package com.choe.CoreTVLauncher

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.choe.CoreTVLauncher.adapter.WallpaperAdapter
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.databinding.ActivityWallpaperPickerBinding

/**
 * 桌布相簿選擇頁面
 *
 * 功能：
 * - 頂部標題列（返回 + 標題 + 「套用」按鈕）
 * - 主區域：4 欄縮圖網格（bg0 預設 + bg1~bg20 + 自選記錄）
 * - 底部：「從裝置選擇圖片或影片」按鈕（SAF 開啟）
 */
class WallpaperPickerActivity : FrostedBaseActivity() {

    companion object {
        fun start(context: android.content.Context) {
            context.startActivity(Intent(context, WallpaperPickerActivity::class.java))
        }

        private const val GRID_COLUMNS = 4
    }

    override val frostedBgViewId: Int get() = R.id.ivFrostedBg

    private lateinit var binding: ActivityWallpaperPickerBinding
    private lateinit var wallpaperAdapter: WallpaperAdapter

    // 目前暫存選擇（尚未套用）
    private var pendingSelection: WallpaperItem = WallpaperItem.Preset(WallpaperManager.DEFAULT_PRESET_INDEX)

    // ─── SAF 檔案選擇器 ────────────────────────────────────────

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getStringExtra("selected_uri") ?: return@registerForActivityResult
            val isVideo = result.data?.getBooleanExtra("is_video", false) ?: false
            val name = result.data?.getStringExtra("selected_name") ?: ""

            val customItem = WallpaperItem.Custom(uri, isVideo, name)
            pendingSelection = customItem

            val pos = wallpaperAdapter.replaceOrAddCustom(customItem)
            binding.rvWallpapers.post {
                binding.rvWallpapers.scrollToPosition(pos)
                binding.rvWallpapers.post {
                    binding.rvWallpapers.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus()
                }
            }

            Toast.makeText(this, "已選取: $name", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── 權限請求 ──────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) openFilePicker()
        else Toast.makeText(this, "需要存取媒體權限才能選擇檔案", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 新增：處理 Android TV 的返回鍵（以及實體返回鍵）
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmDiscardIfNeeded()
            }
        })

        pendingSelection = WallpaperManager.getCurrent(this)
        setupGrid()
        setupButtons()
    }

    // ─── 縮圖網格 ─────────────────────────────────────────────

    private fun buildItemList(): List<WallpaperItem> {
        val list = mutableListOf<WallpaperItem>()
        for (i in 1..WallpaperManager.PRESET_COUNT) {
            list.add(WallpaperItem.Preset(i))
        }
        // 第20格：固定一個自訂義槽位（若已有儲存則顯示）
        val saved = WallpaperManager.getCurrent(this)
        if (saved is WallpaperItem.Custom) {
            list.add(saved)
        }
        return list
    }

    private fun setupGrid() {
        val items = buildItemList()
        wallpaperAdapter = WallpaperAdapter(this, items, pendingSelection) { selected ->
            pendingSelection = selected
        }
        binding.rvWallpapers.apply {
            layoutManager = GridLayoutManager(this@WallpaperPickerActivity, GRID_COLUMNS)
            adapter = wallpaperAdapter
            (itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)
                ?.supportsChangeAnimations = false
            post {
                findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    // ─── 按鈕事件 ─────────────────────────────────────────────

    private fun setupButtons() {
        // 套用
        binding.btnApply.setOnClickListener { applyAndFinish() }

        // 從裝置選擇檔案
        binding.btnOpenFile.setOnClickListener { checkPermissionAndOpenPicker() }
    }

    private fun applyAndFinish() {
        when (val sel = pendingSelection) {
            is WallpaperItem.Preset -> WallpaperManager.savePreset(this, sel.index)
            is WallpaperItem.Custom -> WallpaperManager.saveCustom(
                this, Uri.parse(sel.uriString), sel.isVideo
            )
        }
        Toast.makeText(this, "桌布已套用", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    // ─── 權限 + 檔案選擇 ──────────────────────────────────────

    private fun checkPermissionAndOpenPicker() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) openFilePicker() else permissionLauncher.launch(perms)
    }

    private fun openFilePicker() {
        // 啟動你自己寫的、支援遙控器的檔案選擇視窗
        val intent = Intent(this, FilePickerActivity::class.java)
        filePickerLauncher.launch(intent)
    }


    private fun confirmDiscardIfNeeded() {
        val current = WallpaperManager.getCurrent(this)
        val changed = !isSame(current, pendingSelection)
        if (changed) {
            AlertDialog.Builder(this)
                .setTitle("放棄變更？")
                .setMessage("尚未套用桌布，確定要離開嗎？")
                .setPositiveButton("離開") { _, _ -> finish() }
                .setNegativeButton("繼續選擇", null)
                .show()
        } else {
            finish()
        }
    }

    private fun isSame(a: WallpaperItem, b: WallpaperItem): Boolean = when {
        a is WallpaperItem.Preset && b is WallpaperItem.Preset -> a.index == b.index
        a is WallpaperItem.Custom && b is WallpaperItem.Custom -> a.uriString == b.uriString
        else -> false
    }
}
