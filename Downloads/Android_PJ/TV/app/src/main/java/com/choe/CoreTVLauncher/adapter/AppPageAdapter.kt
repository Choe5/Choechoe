package com.choe.CoreTVLauncher.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.choe.CoreTVLauncher.R
import com.choe.CoreTVLauncher.data.AppInfo

class AppPageAdapter(
    private val context: Context,
    private val allApps: List<AppInfo>
) : RecyclerView.Adapter<AppPageAdapter.PageViewHolder>() {

    companion object {
        const val COLS = 5
        const val PAGE_SIZE = COLS * 3   // 5×3
    }

    val pageCount: Int
        get() = if (allApps.isEmpty()) 0 else (allApps.size + PAGE_SIZE - 1) / PAGE_SIZE

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerView: RecyclerView = view.findViewById(R.id.rvAppGrid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_app_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val start    = position * PAGE_SIZE
        val end      = minOf(start + PAGE_SIZE, allApps.size)
        val pageApps = allApps.subList(start, end)

        holder.recyclerView.apply {
            layoutManager = GridLayoutManager(context, COLS)
            adapter = AppGridAdapter(context, pageApps)
            isNestedScrollingEnabled = false
        }
    }

    override fun getItemCount() = pageCount
}
