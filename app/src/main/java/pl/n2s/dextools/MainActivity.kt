package pl.n2s.dextools

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }
    override fun onResume() { super.onResume(); render() }
    private fun render() {
        val content = Ui.column(this)
        content.addView(Ui.text(this, "n2s  /  NARZĘDZIA DLA DeX", 12f, true))
        content.addView(Ui.title(this, "DeX Tools n2s"))
        content.addView(Ui.text(this, "Mniej przełączania. Więcej kontroli.", 16f, true))
        fun card(title: String, info: String, buttons: List<Pair<String, () -> Unit>>) {
            content.addView(Ui.space(this))
            val panel = Ui.column(this).apply { background = Ui.background() }
            panel.addView(Ui.title(this, title))
            panel.addView(Ui.text(this, info, 15f, true))
            for ((label, action) in buttons) {
                panel.addView(Ui.space(this)); panel.addView(Ui.button(this, label, action), LinearLayout.LayoutParams(-1, -2))
            }
            content.addView(panel)
        }
        card("Screenshot", "Pełny ekran lub prostokąt zaznaczony myszą. PNG trafia do Pictures/Screenshots i jako obraz do schowka. Android wymaga zgody na przechwytywanie przy każdym zrzucie.", listOf(
            "Zrzut całego ekranu" to { CapturePermissionActivity.launch(this, false) },
            "Zaznacz fragment ekranu" to {
                if (Settings.canDrawOverlays(this)) CapturePermissionActivity.launch(this, true) else overlaySettings()
            }
        ))
        card("Text Selection", "Tekst z aktywnego okna jest pobierany wyłącznie na żądanie przez usługę dostępności. Pływający przycisk T otwiera odczytaną treść w osobnym oknie do zaznaczania myszą. Możesz też użyć systemowego Udostępnij lub wkleić tekst ręcznie.", listOf(
            "Otwórz okno tekstu" to { startActivity(Intent(this, TextSelectionActivity::class.java)) },
            "Ustawienia dostępności" to { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        ))
        card("Sterowanie", "Nakładka: ${if (Settings.canDrawOverlays(this)) "dozwolona" else "wymaga zgody"}\nUsługa tekstu: ${if (TextAccessibilityService.instance != null) "połączona" else "wyłączona"}\nCtrl + Shift + S działa w aplikacji; globalnie tylko po włączeniu usługi dostępności i jeśli system przekazuje klawisze. Przeciągaj uchwyt ⋮, aby przenieść panel.", listOf(
            "Zezwól na pływające przyciski" to { overlaySettings() },
            "Włącz pływający panel" to {
                if (Settings.canDrawOverlays(this)) {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
                    }
                    startForegroundService(Intent(this, OverlayService::class.java))
                } else overlaySettings()
            },
            "Wyłącz pływający panel" to { stopService(Intent(this, OverlayService::class.java)) }
        ))
        content.addView(Ui.text(this, "Prywatność: bez sieci, analityki i historii tekstu. Hasła pomijamy. Zrzuty mogą zawierać dane widoczne na ekranie. FLAG_SECURE, treści DRM i ograniczenia systemowe mogą uniemożliwić przechwytywanie. Autor: n2s • 0.1.2", 13f, true))
        Ui.mount(this, ScrollView(this).apply { isFillViewport = true; addView(content) })
    }
    private fun overlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (Shortcuts.isCapture(event.keyCode, event.isCtrlPressed, event.isShiftPressed, event.isAltPressed, event.isMetaPressed)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) CapturePermissionActivity.launch(this, false)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
