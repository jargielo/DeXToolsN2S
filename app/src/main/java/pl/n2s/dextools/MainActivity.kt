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
    private var minimizeAfterPermission = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        minimizeAfterPermission=savedInstanceState?.getBoolean("minimizeAfterPermission") ?: false
        setFinishOnTouchOutside(true)
        if (intent.getBooleanExtra("appearance",false)) {
            intent.removeExtra("appearance")
            window.decorView.post { Appearance.settings(this) }
        }
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
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("minimizeAfterPermission",minimizeAfterPermission)
        super.onSaveInstanceState(outState)
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
        header.addView(Ui.button(this,"−") { enablePanel(true) }.apply {
            contentDescription="Minimalizuj do pływającej kropki"
            minWidth=0; minimumWidth=0; minHeight=0; minimumHeight=0
            setPadding(0,0,0,0); textSize=22f
            setTextColor(Color.WHITE); background=Ui.background(Color.BLACK)
        },LinearLayout.LayoutParams(Ui.dp(this,32),Ui.dp(this,32)))
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
        action("Pływające przyciski") { enablePanel(false) }
        content.addView(Space(this),LinearLayout.LayoutParams(1,Ui.dp(this,8)))
        val footer=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        footer.addView(Ui.button(this,"Ustawienia") { settingsMenu() }.apply {
            textSize=12f; minWidth=0; minimumWidth=0
            setPadding(Ui.dp(context,8),0,Ui.dp(context,8),0)
            background=Ui.background(Color.rgb(30,32,34)); setTextColor(Color.WHITE)
        },LinearLayout.LayoutParams(0,Ui.dp(this,36),1f))
        footer.addView(Ui.text(this,"  n2s • 0.3.0",10f,true))
        content.addView(footer)
        Ui.mount(this,ScrollView(this).apply { addView(content) })
    }
    private fun capture(region: Boolean) {
        CapturePermissionActivity.launch(this,region)
        // Close the toolbox before the frame is acquired, leaving the source visible.
        finish()
    }
    private fun enablePanel(collapsed: Boolean) {
        minimizeAfterPermission=collapsed
        if (!Settings.canDrawOverlays(this)) { overlaySettings(); return }
        if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),7)
            return
        }
        startPanel()
    }
    private fun startPanel() {
        if (!Settings.canDrawOverlays(this)) { overlaySettings(); return }
        startForegroundService(Intent(this,OverlayService::class.java).putExtra("collapsed",minimizeAfterPermission))
        finish()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if (requestCode==7) startPanel()
    }
    private fun settingsMenu() {
        val choices=arrayOf("Przezroczystość","Zgoda na nakładkę","Usługa tekstu i skrótu","Wyłącz pływające przyciski","Zgoda na zrzuty (ADB)","Informacje")
        val dialog=AlertDialog.Builder(this).setTitle("Ustawienia").setItems(choices) { _,which ->
            when(which) {
                0 -> Appearance.settings(this)
                1 -> overlaySettings()
                2 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                3 -> stopService(Intent(this,OverlayService::class.java))
                4 -> adbHelp()
                5 -> info()
            }
        }.create()
        dialog.show()
        dialog.window?.setLayout(Ui.dp(this,280),-2)
    }
    private fun adbHelp() {
        val command="adb shell cmd appops set $packageName PROJECT_MEDIA allow"
        AlertDialog.Builder(this).setTitle("Zgoda na zrzuty przez ADB")
            .setMessage("Opcjonalnie, na swoim komputerze z ADB:\n\n$command\n\nDaje tej aplikacji stałą zgodę na przechwytywanie bez pytania. Test: Android 16 AOSP, bez roota. Samsung / One UI 9 wymaga sprawdzenia.\n\nPrzywrócenie pytania:\nadb shell cmd appops set $packageName PROJECT_MEDIA ignore\n\nZrzuty nadal uruchamiasz ręcznie. Ochrona treści i wskaźniki systemowe pozostają aktywne.")
            .setNeutralButton("Kopiuj polecenie") { _, _ ->
                getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(
                    android.content.ClipData.newPlainText("Zgoda ADB",command))
                Ui.toast(this,"Skopiowano polecenie dla komputera")
            }.setPositiveButton("Zamknij",null).show().window?.setLayout(Ui.dp(this,280),-2)
    }
    private fun info() {
        AlertDialog.Builder(this).setTitle("DeX Tools n2s")
            .setMessage("Autor: n2s\n\nPNG: Pictures/Screenshots i schowek. Domyślnie Android pyta o zgodę na każdy zrzut. Opcjonalna zgoda ADB: Ustawienia → Zgoda na zrzuty (ADB).\n\n− zwija panel do kropki. Kliknij kropkę, aby rozwinąć; przeciągnij, aby przenieść. Przezroczystość: Ustawienia lub ⚙ na panelu.\n\nT odczytuje dostępny tekst. Przytrzymaj T, aby skopiować całe aktywne pole. Ctrl+Shift+S zależy od systemu i usługi dostępności.\n\nBez sieci i historii tekstu. Chronione treści mogą być niedostępne.")
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
