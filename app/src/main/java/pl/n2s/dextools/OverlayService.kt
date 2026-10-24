package pl.n2s.dextools

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: LinearLayout?=null
    override fun onBind(intent: Intent?)=null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action=="STOP") { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        try {
            val notification=Ui.notification(this,"overlay","DeX Tools n2s","Zrzuty i tekst • przytrzymaj T, aby skopiować pole",Intent(this,OverlayService::class.java).setAction("STOP"))
            if (Build.VERSION.SDK_INT>=34) startForeground(11,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(11,notification)
            instance=this
            if (panel==null) createPanel()
        } catch (e: Exception) { Ui.toast(this,"Nie można uruchomić panelu: ${e.message}"); stopSelf() }
        return START_NOT_STICKY
    }
    private fun createPanel() {
        wm=getSystemService(WindowManager::class.java)
        val params=WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.START; x=Ui.dp(this@OverlayService,20); y=Ui.dp(this@OverlayService,100)
        }
        val row=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; background=Ui.background()
            setPadding(Ui.dp(context,6),Ui.dp(context,6),Ui.dp(context,6),Ui.dp(context,6))
            elevation=Ui.dp(context,8).toFloat()
        }
        val handle=Ui.text(this,"⋮",24f).apply {
            contentDescription="Przeciągnij panel"; gravity=Gravity.CENTER; setPadding(0,0,0,0)
        }
        var startX=0; var startY=0; var rawX=0f; var rawY=0f
        handle.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX=params.x; startY=params.y; rawX=event.rawX; rawY=event.rawY }
                MotionEvent.ACTION_MOVE -> {
                    val b=wm.maximumWindowMetrics.bounds
                    params.x=(startX+(event.rawX-rawX).toInt()).coerceIn(0,(b.width()-row.width).coerceAtLeast(0))
                    params.y=(startY+(event.rawY-rawY).toInt()).coerceIn(0,(b.height()-row.height).coerceAtLeast(0))
                    wm.updateViewLayout(row,params)
                }
                MotionEvent.ACTION_UP -> handle.performClick()
            }; true
        }
        row.addView(handle,LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,28)))
        fun button(label: String, description: String, action: () -> Unit): Button {
            return Ui.button(this,label,action).apply {
                contentDescription=description
                minWidth=0; minimumWidth=0; minHeight=0; minimumHeight=0
                setPadding(0,0,0,0); textSize=18f
                row.addView(this,LinearLayout.LayoutParams(Ui.dp(context,44),Ui.dp(context,44)).apply { topMargin=Ui.dp(context,4) })
            }
        }
        button("▣","Zrzut całego ekranu") { capture(false) }
        button("⌗","Zaznacz fragment ekranu") { capture(true) }
        button("T","Odczytaj tekst; przytrzymaj, aby zaznaczyć i skopiować całe aktywne pole") {
            try {
                val service=TextAccessibilityService.instance
                if (service==null) {
                    Ui.toast(this,"Włącz usługę DeX Tools n2s w ustawieniach dostępności")
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    val text=service.snapshot()
                    startActivity(Intent(this,TextSelectionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("snapshot",text).putExtra("emptySnapshot",text.isBlank()))
                }
            } catch (e: Exception) { Ui.toast(this,"Nie udało się odczytać tekstu: ${e.message}") }
        }.setOnLongClickListener {
            val copied=try { TextAccessibilityService.instance?.selectAndCopyFocusedField()==true } catch (_: Exception) { false }
            Ui.toast(this,if(copied) "Zaznaczono i skopiowano całe pole" else "Pole nie pozwala na zaznaczanie lub kopiowanie. Użyj krótkiego kliknięcia T.")
            true
        }
        button("×","Wyłącz panel") { stopSelf() }
        wm.addView(row,params); panel=row
    }
    private fun capture(region: Boolean) {
        try { CapturePermissionActivity.launch(this,region) }
        catch (e: Exception) { show(); Ui.toast(this,"Nie można otworzyć zgody systemowej: ${e.message}") }
    }
    fun hide() { panel?.visibility=View.GONE }
    fun show() { panel?.visibility=View.VISIBLE }
    override fun onDestroy() {
        if (instance===this) instance=null
        panel?.let { if (it.isAttachedToWindow) wm.removeView(it) }; panel=null
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy()
    }
    companion object { var instance: OverlayService?=null; private set }
}
