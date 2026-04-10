package com.choe.CoreTVLauncher.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.choe.CoreTVLauncher.R
import com.choe.CoreTVLauncher.WallpaperManager
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.util.ThumbnailLoader

/**
 * 桌布選擇器 RecyclerView Adapter
 *
 * 縮圖載入：ThumbnailLoader（自定義 BitmapFactory + LruCache，無第三方依賴）
 */
class WallpaperAdapter(
    private val context: Context,
    items: List<WallpaperItem>,
    private var selectedItem: WallpaperItem,
    private val onSelect: (WallpaperItem) -> Unit
) : RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder>() {

    private val items: MutableList<WallpaperItem> = items.toMutableList()

    class WallpaperViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.ivWallpaperThumb)
        val label: TextView = view.findViewById(R.id.tvWallpaperLabel)
        val checkmark: TextView = view.findViewById(R.id.ivCheckmark)
        val videoIcon: TextView = view.findViewById(R.id.ivVideoIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallpaper, parent, false)
        return WallpaperViewHolder(view)
    }

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        val item = items[position]
        val isSelected = isSameItem(item, selectedItem)

        // 1. 清除舊狀態，避免 ViewHolder 複用出錯
        holder.thumbnail.tag = null
        holder.thumbnail.setImageResource(R.drawable.bg_wallpaper)
        holder.checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE

        // 2. 縮圖與標籤載入
        when (item) {
            is WallpaperItem.Preset -> {
                ThumbnailLoader.loadAsset(
                    context,
                    WallpaperManager.assetPath(item.index),
                    holder.thumbnail
                )
                holder.label.text = if (item.index == 1) "預設" else "風格 ${item.index}"
                holder.videoIcon.visibility = View.GONE
            }
            is WallpaperItem.Custom -> {
                ThumbnailLoader.loadUri(
                    context,
                    Uri.parse(item.uriString),
                    holder.thumbnail
                )
                // 顯示檔名
                holder.label.text = item.name.ifEmpty { if (item.isVideo) "自訂影片" else "自訂圖片" }
                holder.videoIcon.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            }
        }

        // ─── 💡 關鍵修正：處理點擊與焦點 (Android TV 必備) ───

        // A. 處理遙控器 OK 鍵 (點擊事件)
        holder.itemView.setOnClickListener {
            android.util.Log.d("WallpaperAdapter", "遙控器點擊了: ${holder.label.text}")
            val oldPos = items.indexOfFirst { isSameItem(it, selectedItem) }
            selectedItem = item
            if (oldPos >= 0) notifyItemChanged(oldPos)
            notifyItemChanged(holder.adapterPosition)
            onSelect(item) // 觸發 Activity 傳入的 Callback
        }

        // B. 處理焦點視覺回饋 (選中放大 + 跑馬燈)
        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            // 讓 TextView 知道現在獲得焦點（啟動 xml 裡的 marquee 跑馬燈）
            holder.label.isSelected = hasFocus

            if (hasFocus) {
                // 選中時稍微放大，並提升高度
                view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                view.elevation = 10f
            } else {
                // 失去焦點時縮回
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                view.elevation = 4f
            }
        }
    }

    override fun getItemCount() = items.size

    /**
     * 自訂欄位（固定最後一格）：存在則替換，不存在則新增。
     * 回傳該欄位的 position，供呼叫端設定焦點。
     */
    fun replaceOrAddCustom(item: WallpaperItem.Custom): Int {
        val pos = items.indexOfFirst { it is WallpaperItem.Custom }
        return if (pos >= 0) {
            items[pos] = item
            notifyItemChanged(pos)
            pos
        } else {
            items.add(item)
            notifyItemInserted(items.size - 1)
            items.size - 1
        }
    }

    private fun isSameItem(a: WallpaperItem, b: WallpaperItem): Boolean = when {
        a is WallpaperItem.Preset && b is WallpaperItem.Preset -> a.index == b.index
        a is WallpaperItem.Custom && b is WallpaperItem.Custom -> a.uriString == b.uriString
        else -> false
    }
}
