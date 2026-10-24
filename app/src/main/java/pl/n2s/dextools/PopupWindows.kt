package pl.n2s.dextools

import android.app.Activity
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager

object PopupWindows {
    fun fit(activity: Activity, widthDp: Int, heightDp: Int) {
        val metrics = activity.windowManager.maximumWindowMetrics
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val margin = Ui.dp(activity, 24)
        val width = minOf(Ui.dp(activity, widthDp), metrics.bounds.width() - bars.left - bars.right - margin)
        val height = minOf(Ui.dp(activity, heightDp), metrics.bounds.height() - bars.top - bars.bottom - margin)
        activity.window.apply {
            setGravity(Gravity.CENTER)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            setDimAmount(0f)
            setLayout(width.coerceAtLeast(1), height.coerceAtLeast(1))
        }
    }
}
