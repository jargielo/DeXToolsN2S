package pl.n2s.dextools

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.LinearLayout
import android.widget.SeekBar

object Appearance {
    private fun prefs(c: Context) = c.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun alpha(c: Context) = (100 - prefs(c).getInt("transparency", 0).coerceIn(0, 80)) / 100f
    fun apply(activity: Activity) {
        activity.window.attributes = activity.window.attributes.apply { alpha = alpha(activity) }
    }
    fun settings(activity: Activity) {
        val content = Ui.column(activity)
        val label = Ui.text(activity, "", 14f)
        content.addView(label)
        fun update(value: Int) {
            label.text = "Przezroczystość: $value%"
            prefs(activity).edit().putInt("transparency", value).apply()
            apply(activity)
            OverlayService.instance?.refreshOpacity()
        }
        content.addView(SeekBar(activity).apply {
            max = 80
            progress = prefs(activity).getInt("transparency", 0).coerceIn(0, 80)
            label.text = "Przezroczystość: $progress%"
            contentDescription = "Przezroczystość okien, panelu i kropki"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) update(value) }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }, LinearLayout.LayoutParams(-1, Ui.dp(activity, 48)))
        content.addView(Ui.text(activity, "Dotyczy okien narzędzi, panelu i kropki. Minimum 20% widoczności.", 12f, true))
        AlertDialog.Builder(activity).setTitle("Wygląd").setView(content)
            .setNeutralButton("Nieprzezroczyste") { _, _ -> update(0) }
            .setPositiveButton("Gotowe", null).show().window?.setLayout(Ui.dp(activity, 280), -2)
    }
}
