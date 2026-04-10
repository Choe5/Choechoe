package com.choe.CoreTVLauncher.adapter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.choe.CoreTVLauncher.R
import com.choe.CoreTVLauncher.data.AppInfo
import com.choe.CoreTVLauncher.worker.WeatherWorker

class AppGridAdapter(
    private val context: Context,
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(WeatherWorker.PREF_NAME, Context.MODE_PRIVATE)

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivAppIcon)
        val label: TextView = view.findViewById(R.id.tvAppLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.label.visibility = View.INVISIBLE

        // 點擊啟動 APP
        holder.itemView.setOnClickListener {
            context.packageManager
                .getLaunchIntentForPackage(app.packageName)
                ?.let { context.startActivity(it) }
        }

        // 長按顯示上下文選單
        holder.itemView.setOnLongClickListener {
            showContextMenu(app)
            true
        }

        // focus：放大 + 顯示名稱（無外框）
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            holder.label.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
            v.animate()
                .scaleX(if (hasFocus) 1.18f else 1f)
                .scaleY(if (hasFocus) 1.18f else 1f)
                .setDuration(140)
                .start()
        }
    }

    override fun getItemCount() = apps.size

    // ─── 長按上下文選單 ───────────────────────────────────────────

    private fun showContextMenu(app: AppInfo) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_app_context)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // 底部全寬浮層
            setGravity(Gravity.BOTTOM)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            // 入場動畫：從下往上滑入
            attributes = attributes.also { it.windowAnimations = android.R.style.Animation_InputMethod }
        }

        // 刪除 APP
        dialog.findViewById<LinearLayout>(R.id.btnCtxDelete).apply {
            setOnClickListener {
                dialog.dismiss()
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:${app.packageName}")
                }
                context.startActivity(intent)
            }
            setOnFocusChangeListener { v, has -> scaleOnFocus(v, has) }
            requestFocus()
        }

        // 設為影片捷徑
        dialog.findViewById<LinearLayout>(R.id.btnCtxVideo).apply {
            setOnClickListener {
                dialog.dismiss()
                prefs.edit()
                    .putString("shortcut_video", app.packageName)
                    .putString("shortcut_video_label", app.label)
                    .apply()
                Toast.makeText(context, "已設為影片應用：${app.label}", Toast.LENGTH_SHORT).show()
            }
            setOnFocusChangeListener { v, has -> scaleOnFocus(v, has) }
        }

        // 設為音樂捷徑
        dialog.findViewById<LinearLayout>(R.id.btnCtxMusic).apply {
            setOnClickListener {
                dialog.dismiss()
                prefs.edit()
                    .putString("shortcut_music", app.packageName)
                    .putString("shortcut_music_label", app.label)
                    .apply()
                Toast.makeText(context, "已設為音樂應用：${app.label}", Toast.LENGTH_SHORT).show()
            }
            setOnFocusChangeListener { v, has -> scaleOnFocus(v, has) }
        }

        // 返回
        dialog.findViewById<LinearLayout>(R.id.btnCtxBack).apply {
            setOnClickListener { dialog.dismiss() }
            setOnFocusChangeListener { v, has -> scaleOnFocus(v, has) }
        }

        dialog.show()
    }

    private fun scaleOnFocus(v: View, hasFocus: Boolean) {
        v.animate()
            .scaleX(if (hasFocus) 1.15f else 1f)
            .scaleY(if (hasFocus) 1.15f else 1f)
            .setDuration(130)
            .start()
    }
}
