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
    private lateinit var params: WindowManager.LayoutParams
    private val preferences by lazy { getSharedPreferences("appearance", MODE_PRIVATE) }
    private var collapsed = false
    override fun onBind(intent: Intent?)=null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action=="STOP") { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        try {
            val notification=Ui.notification(this,"overlay","DeX Tools n2s","Zrzuty i tekst • przytrzymaj T, aby skopiować pole",Intent(this,OverlayService::class.java).setAction("STOP"))
            if (Build.VERSION.SDK_INT>=34) startForeground(11,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(11,notification)
            instance=this
            collapsed = if (intent?.hasExtra("collapsed")==true) intent.getBooleanExtra("collapsed",false)
                else preferences.getBoolean("collapsed",false)
            if (panel==null) createPanel() else renderPanel()
        } catch (e: Exception) { Ui.toast(this,"Nie można uruchomić panelu: ${e.message}"); stopSelf() }
        return START_NOT_STICKY
    }
    private fun createPanel() {
        wm=getSystemService(WindowManager::class.java)
        params=WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.START
            x=preferences.getInt("x",Ui.dp(this@OverlayService,20))
            y=preferences.getInt("y",Ui.dp(this@OverlayService,100))
            alpha=Appearance.alpha(this@OverlayService)
        }
        val row=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        panel=row
        wm.addView(row,params)
        renderPanel()
    }
    private fun renderPanel() {
        val row=panel ?: return
        preferences.edit().putBoolean("collapsed",collapsed).apply()
        row.removeAllViews()
        val padding=Ui.dp(this,if(collapsed) 8 else 6)
        row.setPadding(padding,padding,padding,padding)
        row.background=if(collapsed) null else Ui.background()
        row.elevation=if(collapsed) 0f else Ui.dp(this,8).toFloat()
        if (collapsed) {
            val dot=TextView(this).apply {
                background=Ui.background(Ui.accent,Ui.dp(context,16).toFloat())
                contentDescription="Rozwiń panel DeX Tools; przeciągnij, aby przenieść kropkę"
                isFocusable=true
            }
            row.addView(dot,LinearLayout.LayoutParams(Ui.dp(this,32),Ui.dp(this,32)))
            // The whole 48 dp window is a hit target, including the margin around the dot.
            row.contentDescription=dot.contentDescription
            draggable(row) { setCollapsed(false) }
            draggable(dot) { setCollapsed(false) }
        } else {
            row.contentDescription="Panel DeX Tools"
            row.setOnTouchListener(null)
            row.setOnClickListener(null)
            val handle=Ui.text(this,"⋮",24f).apply {
                contentDescription="Przeciągnij panel"; gravity=Gravity.CENTER; setPadding(0,0,0,0)
            }
            row.addView(handle,LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,28)))
            draggable(handle) {}
            addButtons(row)
        }
        row.post { clampPosition(); refreshOpacity() }
    }
    private fun addButtons(row: LinearLayout) {
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
        button("−","Minimalizuj do pływającej kropki") { setCollapsed(true) }
        button("⚙","Ustaw przezroczystość") {
            startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("appearance",true))
        }
        button("×","Wyłącz panel") { stopSelf() }
    }
    private fun setCollapsed(value: Boolean) { collapsed=value; renderPanel() }
    fun refreshOpacity() {
        val row=panel ?: return
        if (row.isAttachedToWindow) {
            params.alpha=Appearance.alpha(this)
            wm.updateViewLayout(row,params)
        }
    }
    private fun clampPosition(persist: Boolean = true) {
        val row=panel ?: return
        val metrics=wm.maximumWindowMetrics
        val bars=metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        // LayoutParams offsets are relative to the inset parent, not the physical screen origin.
        params.x=params.x.coerceIn(0,(metrics.bounds.width()-bars.left-bars.right-row.width).coerceAtLeast(0))
        params.y=params.y.coerceIn(0,(metrics.bounds.height()-bars.top-bars.bottom-row.height).coerceAtLeast(0))
        if (row.isAttachedToWindow) wm.updateViewLayout(row,params)
        if (persist) preferences.edit().putInt("x",params.x).putInt("y",params.y).apply()
    }
    private fun draggable(view: View, click: () -> Unit) {
        val slop=ViewConfiguration.get(this).scaledTouchSlop
        var startX=0; var startY=0; var rawX=0f; var rawY=0f; var moved=false
        view.setOnClickListener { click() }
        view.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX=params.x; startY=params.y; rawX=event.rawX; rawY=event.rawY; moved=false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx=event.rawX-rawX; val dy=event.rawY-rawY
                    if (kotlin.math.abs(dx)>slop || kotlin.math.abs(dy)>slop) moved=true
                    if (moved) { params.x=startX+dx.toInt(); params.y=startY+dy.toInt(); clampPosition(false) }
                }
                MotionEvent.ACTION_UP -> {
                    val dx=event.rawX-rawX; val dy=event.rawY-rawY
                    if (moved || kotlin.math.abs(dx)>slop || kotlin.math.abs(dy)>slop) {
                        params.x=startX+dx.toInt(); params.y=startY+dy.toInt(); clampPosition()
                    } else view.performClick()
                }
                MotionEvent.ACTION_CANCEL -> { moved=false; clampPosition() }
            }
            true
        }
    }
    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        super.onConfigurationChanged(config)
        panel?.post { clampPosition() }
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
