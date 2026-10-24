package pl.n2s.dextools

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.WindowInsets
import android.widget.*

object Ui {
    val accent = Color.rgb(165, 243, 208)
    fun dp(c: Context, n: Int) = (n * c.resources.displayMetrics.density).toInt()
    fun background(color: Int = Color.rgb(18, 20, 22), radius: Float = 20f) =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }
    fun text(c: Context, value: String, size: Float = 16f, muted: Boolean = false) = TextView(c).apply {
        text = value; textSize = size; setTextColor(if (muted) Color.LTGRAY else Color.WHITE)
        setPadding(0, dp(c, 8), 0, dp(c, 8))
    }
    fun button(c: Context, label: String, action: () -> Unit) = Button(c).apply {
        text = label; isAllCaps = false; setTextColor(Color.BLACK)
        background = background(accent, dp(c, 12).toFloat())
        minHeight = dp(c, 48)
        setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8))
        setOnClickListener { action() }
    }
    fun column(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c, 24), dp(c, 16), dp(c, 24), dp(c, 24))
    }
    fun space(c: Context) = Space(c).apply { layoutParams = LinearLayout.LayoutParams(1, dp(c, 12)) }
    fun mount(a: Activity, content: View) {
        val attributes = a.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowIsFloating))
        val floating = attributes.getBoolean(0,false)
        attributes.recycle()
        val root = FrameLayout(a).apply {
            if (floating) {
                background=background(Color.BLACK,dp(a,16).toFloat()).apply { setStroke(dp(a,1),Color.rgb(48,52,55)) }
                clipToOutline=true
            } else setBackgroundColor(Color.BLACK)
            addView(content, FrameLayout.LayoutParams(-1, -1))
            if (!floating) setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        a.setContentView(root)
        root.requestApplyInsets()
    }
    fun title(c: Context, s: String) = text(c, s, 28f).apply { setTypeface(null, Typeface.BOLD) }
    fun toast(c: Context, s: String) = Toast.makeText(c, s, Toast.LENGTH_LONG).show()
    fun notification(c: Context, channel: String, title: String, text: String, stop: Intent): Notification {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channel, "Narzędzia DeX", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val end = PendingIntent.getService(c, channel.hashCode(), stop, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(c, channel).setSmallIcon(R.drawable.ic_app)
            .setContentTitle(title).setContentText(text).setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Zatrzymaj", end).build()).build()
    }
}
