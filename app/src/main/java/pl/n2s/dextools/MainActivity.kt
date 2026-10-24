package pl.n2s.dextools

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.widget.*
import java.lang.ref.WeakReference

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
    }
    override fun onResume() {
        super.onResume()
        activePopup=WeakReference(this)
        render()
        PopupWindows.fit(this, 280, 344)
    }
    override fun onDestroy() {
        if (activePopup.get()===this) activePopup.clear()
        super.onDestroy()
    }
    companion object {
        private var activePopup=WeakReference<MainActivity>(null)
        fun dismissForCapture() { activePopup.get()?.finish() }
    }
    private fun render() {
        val content = Ui.column(this).apply {
            setPadding(Ui.dp(context,16),Ui.dp(context,12),Ui.dp(context,16),Ui.dp(context,12))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(Ui.text(this,"DeX Tools n2s",20f),LinearLayout.LayoutParams(0,-2,1f))
        header.addView(Ui.button(this,"×") { finish() }.apply {
            contentDescription="Zamknij okienko"
            minWidth=0; minimumWidth=0; minHeight=0; minimumHeight=0
            setPadding(0,0,0,0); textSize=22f
            setTextColor(Color.WHITE); background=Ui.background(Color.BLACK)
        },LinearLayout.LayoutParams(Ui.dp(this,32),Ui.dp(this,32)))
        content.addView(header)
        fun action(label: String, run: () -> Unit) {
            content.addView(Space(this),LinearLayout.LayoutParams(1,Ui.dp(this,6)))
            content.addView(Ui.button(this,label,run).apply {
                minHeight=Ui.dp(context,44); minimumHeight=Ui.dp(context,44); textSize=14f
            },LinearLayout.LayoutParams(-1,Ui.dp(this,44)))
        }
        action("Zrzut całego ekranu") { capture(false) }
        action("Zaznacz fragment") {
            if (Settings.canDrawOverlays(this)) capture(true) else overlaySettings()
        }
        action("Okno tekstu") {
            startActivity(Intent(this,TextSelectionActivity::class.java))
            finish()
        }
        action("Pływające przyciski") { enablePanel() }
        content.addView(Space(this),LinearLayout.LayoutParams(1,Ui.dp(this,8)))
        val footer=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        footer.addView(Ui.button(this,"Ustawienia") { settingsMenu() }.apply {
            textSize=12f; minWidth=0; minimumWidth=0
            setPadding(Ui.dp(context,8),0,Ui.dp(context,8),0)
            background=Ui.background(Color.rgb(30,32,34)); setTextColor(Color.WHITE)
        },LinearLayout.LayoutParams(0,Ui.dp(this,36),1f))
        footer.addView(Ui.text(this,"  n2s • 0.2.0",10f,true))
        content.addView(footer)
        Ui.mount(this,ScrollView(this).apply { addView(content) })
    }
    private fun capture(region: Boolean) {
        CapturePermissionActivity.launch(this,region)
        // Close the toolbox before the frame is acquired, leaving the source visible.
        finish()
    }
    private fun enablePanel() {
        if (!Settings.canDrawOverlays(this)) { overlaySettings(); return }
        if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),7)
            return
        }
        startPanel()
    }
    private fun startPanel() {
        if (!Settings.canDrawOverlays(this)) { overlaySettings(); return }
        startForegroundService(Intent(this,OverlayService::class.java))
        finish()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if (requestCode==7) startPanel()
    }
    private fun settingsMenu() {
        val choices=arrayOf("Zgoda na nakładkę","Usługa tekstu i skrótu","Wyłącz pływające przyciski","Informacje")
        val dialog=AlertDialog.Builder(this).setTitle("Ustawienia").setItems(choices) { _,which ->
            when(which) {
                0 -> overlaySettings()
                1 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                2 -> stopService(Intent(this,OverlayService::class.java))
                3 -> info()
            }
        }.create()
        dialog.show()
        dialog.window?.setLayout(Ui.dp(this,280),-2)
    }
    private fun info() {
        AlertDialog.Builder(this).setTitle("DeX Tools n2s")
            .setMessage("Autor: n2s\n\nPNG: Pictures/Screenshots i schowek. Android wymaga zgody na każdy zrzut.\n\nT odczytuje dostępny tekst. Przytrzymaj T, aby skopiować całe aktywne pole. Ctrl+Shift+S zależy od systemu i usługi dostępności.\n\nBez sieci i historii tekstu. Chronione treści mogą być niedostępne.")
            .setPositiveButton("Zamknij",null).show().window?.setLayout(Ui.dp(this,280),-2)
    }
    private fun overlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (Shortcuts.isCapture(event.keyCode,event.isCtrlPressed,event.isShiftPressed,event.isAltPressed,event.isMetaPressed)) {
            if (event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0) capture(false)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
