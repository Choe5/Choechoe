package com.choe.CoreTVLauncher.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.choe.CoreTVLauncher.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class ArcMenuLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    data class ArcItem(
        val icon: String,
        val label: String,
        val color: Int = 0xFF4444FF.toInt(), // 預設藍色
        val action: () -> Unit
    )

    companion object {
        private const val VISIBLE_SLOTS  = 5      // 增加槽位，實現流暢轉入
        private const val EXPAND_DUR     = 350L
        private const val COLLAPSE_DUR   = 250L
        private const val CYCLE_DUR      = 300L

        private const val TRIGGER_MARGIN = 24f    // dp
        private const val ARC_RADIUS     = 120f   // 加大半徑，讓 icon 轉動幅度變大
        private const val TRIGGER_SIZE   = 40f    // dp
        private const val LABEL_GAP      = 20f    // dp

        // 步進角度：45度 (π/4)
        private const val ANGLE_STEP = Math.PI / 4.0
        // 中心選中角度：45度 (右上)
        private const val CENTER_ANGLE = Math.PI / 4.0
    }

    var isExpanded = false
    private var isAnimating = false
    private var expandFrac = 0f
    private var currentIndex = 0
    private var allItems: List<ArcItem> = emptyList()

    // 關鍵：目前的旋轉偏移量（弧度）
    private var currentAngleOffset = 0.0

    private lateinit var triggerView: View
    private val slotViews = arrayOfNulls<View>(VISIBLE_SLOTS)
    private lateinit var labelView: TextView

    private val dp   get() = resources.displayMetrics.density
    private val tMgn get() = (TRIGGER_MARGIN * dp).toInt()
    private val rad  get() = ARC_RADIUS * dp
    private val tSz  get() = (TRIGGER_SIZE  * dp).toInt()
    private val lGap get() = (LABEL_GAP     * dp)

    init {
        buildViews()
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun buildViews() {
        val inf = LayoutInflater.from(context)

        // 1. 觸發按鈕
        triggerView = inf.inflate(R.layout.item_arc_button, this, false).apply {
            background = context.getDrawable(R.drawable.bg_arc_trigger)
            //findViewById<TextView>(R.id.tvArcIcon)?.text = ""

            // 把動畫代碼貼在這裡：
            val blinker = android.animation.ObjectAnimator.ofFloat(this, "alpha", 0.4f, 1.0f).apply {
                duration = 1000L
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
            }
            blinker.start()

            setOnClickListener { toggle() }
        }
        addView(triggerView)

        // 2. 5個槽位 View
        repeat(VISIBLE_SLOTS) { s ->
            val sv = inf.inflate(R.layout.item_arc_button, this, false).apply {
                background = context.getDrawable(R.drawable.bg_arc_item)
                visibility = View.GONE
            }
            slotViews[s] = sv
            addView(sv)
        }

        // 3. 標籤
        labelView = TextView(context).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            visibility = View.GONE
        }
        addView(labelView)
    }

    fun setItems(items: List<ArcItem>) {
        allItems = items
        currentIndex = 0
        refreshSlots()
    }

    fun toggle() { if (isExpanded) collapse() else expand() }

    fun expand() {
        if (isAnimating || isExpanded || allItems.isEmpty()) return
        isExpanded = true
        isAnimating = true
        currentAngleOffset = 0.0
        refreshSlots()

        slotViews.forEach { it?.visibility = View.VISIBLE }
        triggerView.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200).start()

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = EXPAND_DUR
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                expandFrac = it.animatedValue as Float
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator) {
                    isAnimating = false
                    showLabel(true)
                }
            })
            start()
        }
        requestFocus()
    }

    fun collapse() {
        if (isAnimating || !isExpanded) return
        isAnimating = true
        labelView.visibility = View.GONE

        triggerView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200).start()

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = COLLAPSE_DUR
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                expandFrac = it.animatedValue as Float
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator) {
                    isExpanded = false
                    isAnimating = false
                    slotViews.forEach { it?.visibility = View.GONE }
                    triggerView.requestFocus()
                }
            })
            start()
        }
    }
    fun requestTriggerFocus() {
        triggerView.requestFocus()
    }
    private fun realScroll(direction: Int) {
        if (isAnimating || allItems.isEmpty()) return
        isAnimating = true
        labelView.animate().alpha(0f).setDuration(100).start()

        // 方向補正：想要項目向上轉，角度應增加
        val startAngle = 0.0
        val targetAngle = direction * ANGLE_STEP

        ValueAnimator.ofFloat(startAngle.toFloat(), targetAngle.toFloat()).apply {
            duration = CYCLE_DUR
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener {
                currentAngleOffset = (it.animatedValue as Float).toDouble()
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator) {
                    currentIndex = (currentIndex + direction + allItems.size) % allItems.size
                    currentAngleOffset = 0.0
                    refreshSlots()
                    isAnimating = false
                    requestLayout()
                    showLabel(true)
                }
            })
            start()
        }
    }

    private fun refreshSlots() {
        val n = allItems.size
        if (n == 0) return
        for (s in 0 until VISIBLE_SLOTS) {
            val idx = (currentIndex + (s - 2) + n) % n
            val sv = slotViews[s] ?: continue
            val item = allItems[idx]

            // 設置圖示文字
            sv.findViewById<TextView>(R.id.tvArcIcon)?.text = item.icon

            // 核心修改：動態設置背景顏色
            // 使用 backgroundTintList 不會破壞原本的圓形外觀（Shape）
            sv.backgroundTintList = android.content.res.ColorStateList.valueOf(item.color)
        }
    }

    private fun showLabel(animate: Boolean) {
        labelView.text = allItems[currentIndex].label
        labelView.visibility = View.VISIBLE
        if (animate) {
            labelView.alpha = 0f
            labelView.animate().alpha(1f).setDuration(200).start()
        } else labelView.alpha = 1f
    }

    override fun onMeasure(w: Int, h: Int) {
        super.onMeasure(w, h)
        for (i in 0 until childCount) measureChild(getChildAt(i), w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 1. 觸發按鈕位置
        val ts = tSz
        val tL = tMgn
        val tT = height - ts - tMgn
        triggerView.layout(tL, tT, tL + ts, tT + ts)

        val originX = tL + ts / 2f
        val originY = tT + ts / 2f

        // 2. 5個槽位的物理位置計算
        for (s in 0 until VISIBLE_SLOTS) {
            val sv = slotViews[s] ?: continue
            if (sv.visibility != View.VISIBLE) continue

            // 物理角度 = 基礎角度 + 旋轉偏移
            val angle = (s - 2) * ANGLE_STEP + CENTER_ANGLE - currentAngleOffset

            val dist = rad * expandFrac
            val cx = originX + dist * cos(angle).toFloat()
            val cy = originY - dist * sin(angle).toFloat()

            sv.layout(
                (cx - sv.measuredWidth / 2f).toInt(), (cy - sv.measuredHeight / 2f).toInt(),
                (cx + sv.measuredWidth / 2f).toInt(), (cy + sv.measuredHeight / 2f).toInt()
            )

            // 視覺優化：根據離中心角度的距離計算縮放和透明度
            val diff = abs(angle - CENTER_ANGLE)
            val scale = (1.0f - (diff.toFloat() * 0.5f)).coerceIn(0.4f, 1.0f) * expandFrac
            sv.scaleX = scale
            sv.scaleY = scale
            sv.alpha = (1.0f - (diff.toFloat() * 0.2f)).coerceIn(0f, 1f) * expandFrac

            // 選中項高亮
            sv.background = context.getDrawable(
                if (diff < 0.2) R.drawable.bg_arc_item_selected else R.drawable.bg_arc_item
            )
        }

        // 3. 標籤位置（對齊選中項，即槽位 2）
        val cv = slotViews[2]
        if (labelView.visibility == View.VISIBLE && cv != null && expandFrac > 0.5f) {
            val lL = (cv.right + lGap).toInt()
            val lT = cv.top + (cv.height - labelView.measuredHeight) / 2
            labelView.layout(lL, lT, lL + labelView.measuredWidth, lT + labelView.measuredHeight)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!isAnimating) {
                    if (isExpanded) {
                        val item = allItems[currentIndex]
                        collapse()
                        postDelayed({ item.action() }, 100)
                    } else expand()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isExpanded) { realScroll(1); true } else super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isExpanded) { realScroll(-1); true } else super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isExpanded) { collapse(); true } else super.dispatchKeyEvent(event)
            }
            else -> super.dispatchKeyEvent(event)
        }
    }
}