package com.choe.CoreTVLauncher

import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.choe.CoreTVLauncher.adapter.AppPageAdapter
import com.choe.CoreTVLauncher.data.AppRepository
import com.choe.CoreTVLauncher.data.WallpaperItem
import com.choe.CoreTVLauncher.databinding.ActivityAppDrawerBinding
import com.choe.CoreTVLauncher.util.ThumbnailLoader

class AppDrawerActivity : AppCompatActivity(), AppRepository.OnAppsChangedListener {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AppDrawerActivity::class.java))
        }
        private const val COLS = AppPageAdapter.COLS
    }

    private lateinit var binding: ActivityAppDrawerBinding
    private lateinit var appRepository: AppRepository
    private val dotViews = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appRepository = (application as ArcFlowApp).appRepository
        appRepository.addListener(this)

        applyBlurBackground()

        binding.shimmerLayout.startShimmer()
        binding.contentLayout.visibility = View.GONE

        appRepository.loadAppsAsync {
            if (!isDestroyed) setupViewPager()
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.shimmerLayout.visibility == View.VISIBLE)
            binding.shimmerLayout.startShimmer()
    }

    override fun onPause() {
        super.onPause()
        binding.shimmerLayout.stopShimmer()
    }

    override fun onDestroy() {
        super.onDestroy()
        appRepository.removeListener(this)
    }

    // ─── 翻頁：在 Activity 層攔截邊界鍵，ViewPager2 setCurrentItem ──

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        if (binding.contentLayout.visibility != View.VISIBLE) return super.dispatchKeyEvent(event)

        val rv = currentPageRecyclerView() ?: return super.dispatchKeyEvent(event)
        val focused = rv.focusedChild ?: return super.dispatchKeyEvent(event)
        val pos = rv.getChildAdapterPosition(focused)
        if (pos == RecyclerView.NO_POSITION) return super.dispatchKeyEvent(event)

        val itemCount = rv.adapter?.itemCount ?: return super.dispatchKeyEvent(event)
        val totalPages = binding.viewPager.adapter?.itemCount ?: 1

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 最右欄 或 最後一個 item
                val atEdge = (pos % COLS == COLS - 1) || (pos == itemCount - 1)
                if (atEdge) {
                    val next = binding.viewPager.currentItem + 1
                    if (next < totalPages) {
                        binding.viewPager.setCurrentItem(next, true)
                        return true
                    }
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // 最左欄
                if (pos % COLS == 0) {
                    val prev = binding.viewPager.currentItem - 1
                    if (prev >= 0) {
                        binding.viewPager.setCurrentItem(prev, true)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** 取得目前可見頁面的 RecyclerView */
    private fun currentPageRecyclerView(): RecyclerView? {
        val page = binding.viewPager.currentItem
        // ViewPager2 的子 RecyclerView 可透過 findViewWithTag 或直接遍歷取得
        for (i in 0 until binding.viewPager.childCount) {
            val child = binding.viewPager.getChildAt(i)
            if (child is RecyclerView) {
                // ViewPager2 內部包一層 RecyclerView，再下一層才是我們的 page view
                val vh = child.findViewHolderForAdapterPosition(page)
                val pageRv = vh?.itemView?.findViewById<RecyclerView>(R.id.rvAppGrid)
                if (pageRv != null) return pageRv
            }
        }
        return null
    }

    // ─── 霧化背景 ────────────────────────────────────────────────

    private fun applyBlurBackground() {
        when (val wp = WallpaperManager.getCurrent(this)) {
            is WallpaperItem.Preset -> ThumbnailLoader.loadAsset(
                this, WallpaperManager.assetPath(wp.index), binding.ivBlurBg, targetW = 960
            )
            is WallpaperItem.Custom -> if (!wp.isVideo) ThumbnailLoader.loadUri(
                this, Uri.parse(wp.uriString), binding.ivBlurBg, targetW = 960
            ) else binding.ivBlurBg.setBackgroundColor(0xFF0D0D1A.toInt())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.ivBlurBg.setRenderEffect(
                RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP)
            )
            binding.vFrostOverlay.setBackgroundColor(0x88000000.toInt())
        }
    }

    // ─── ViewPager2 ──────────────────────────────────────────────

    private fun setupViewPager() {
        val apps    = appRepository.apps
        val adapter = AppPageAdapter(this, apps)

        binding.viewPager.apply {
            this.adapter = adapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateDots(position)
                    // 翻頁後把焦點移到新頁第一個 item
                    postDelayed({ focusFirstItemOnPage(position) }, 200)
                }
            })
        }

        buildDots(adapter.pageCount)
        updateDots(0)

        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE

        // 初始焦點定位到第一頁第一個 APP
        binding.viewPager.post { focusFirstItemOnPage(0) }
    }

    /** 將焦點移到指定頁第一個 APP item */
    private fun focusFirstItemOnPage(page: Int) {
        val rv = currentPageRecyclerViewByPage(page) ?: return
        rv.post {
            val first = rv.findViewHolderForAdapterPosition(0)?.itemView
            first?.requestFocus()
        }
    }

    private fun currentPageRecyclerViewByPage(page: Int): RecyclerView? {
        for (i in 0 until binding.viewPager.childCount) {
            val child = binding.viewPager.getChildAt(i)
            if (child is RecyclerView) {
                val vh = child.findViewHolderForAdapterPosition(page)
                return vh?.itemView?.findViewById(R.id.rvAppGrid)
            }
        }
        return null
    }

    // ─── 圓點指示器 ──────────────────────────────────────────────

    private fun buildDots(count: Int) {
        dotViews.clear()
        binding.dotsIndicator.removeAllViews()
        if (count <= 1) return
        val sizePx   = (10 * resources.displayMetrics.density).toInt()
        val marginPx = (6  * resources.displayMetrics.density).toInt()
        repeat(count) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.dot_unselected)
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
            }
            binding.dotsIndicator.addView(dot)
            dotViews.add(dot)
        }
    }

    private fun updateDots(page: Int) {
        dotViews.forEachIndexed { i, dot ->
            dot.setImageResource(if (i == page) R.drawable.dot_selected else R.drawable.dot_unselected)
        }
    }

    override fun onAppsChanged() {
        runOnUiThread { setupViewPager() }
    }
}
