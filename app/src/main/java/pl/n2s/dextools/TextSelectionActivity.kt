package pl.n2s.dextools

import android.app.Activity
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.*

class TextSelectionActivity : Activity() {
    private lateinit var editor: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout=Ui.column(this).apply { setPadding(Ui.dp(context,16),Ui.dp(context,12),Ui.dp(context,16),Ui.dp(context,12)) }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(Ui.text(this,"Zaznacz tekst",20f),LinearLayout.LayoutParams(0,-2,1f))
        header.addView(Ui.button(this,"×") { finish() }.apply {
            contentDescription="Zamknij okno tekstu"
            minWidth=0; minimumWidth=0; minHeight=0; minimumHeight=0
            setPadding(0,0,0,0); textSize=22f; setTextColor(Color.WHITE); background=Ui.background(Color.BLACK)
        },LinearLayout.LayoutParams(Ui.dp(this,32),Ui.dp(this,32)))
        layout.addView(header)
        layout.addView(Ui.text(this,"Mysz: przeciągnij · Ctrl+A/C/V",12f,true))
        editor=EditText(this).apply {
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            hint="Użyj T nad źródłem, udostępnij lub wklej tekst."
            textSize=16f; gravity=Gravity.TOP or Gravity.START
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSelectAllOnFocus(false); isSaveEnabled=false
            background=Ui.background()
            setPadding(Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12))
        }
        enableMouseSelection(editor)
        layout.addView(editor,LinearLayout.LayoutParams(-1,0,1f))
        layout.addView(Ui.space(this))
        val actions=LinearLayout(this)
        fun action(label: String, run: () -> Unit) {
            actions.addView(Ui.button(this,label,run).apply {
                textSize=12f; minWidth=0; minimumWidth=0; minHeight=0; minimumHeight=0
                setPadding(Ui.dp(context,4),0,Ui.dp(context,4),0)
            },LinearLayout.LayoutParams(0,Ui.dp(this,40),1f).apply { marginEnd=Ui.dp(this@TextSelectionActivity,4) })
        }
        action("Kopiuj") {
            val a=minOf(editor.selectionStart,editor.selectionEnd).coerceAtLeast(0)
            val b=maxOf(editor.selectionStart,editor.selectionEnd).coerceAtLeast(0)
            if (a==b) Ui.toast(this,"Najpierw zaznacz tekst")
            else {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Tekst — DeX Tools n2s",editor.text.subSequence(a,b)))
                Ui.toast(this,"Skopiowano tekst")
            }
        }
        action("Całość") { editor.requestFocus(); editor.selectAll() }
        action("Wyczyść") { editor.text.clear() }
        layout.addView(actions)
        Ui.mount(this,layout)
        PopupWindows.fit(this,320,440)
        receive(intent)
    }
    private fun enableMouseSelection(field: EditText) {
        var anchor: Int?=null
        field.setOnTouchListener { _,event ->
            if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return@setOnTouchListener false
            fun offset()=field.getOffsetForPosition(event.x,event.y).coerceIn(0,field.text.length)
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.buttonState!=0 && event.buttonState and MotionEvent.BUTTON_PRIMARY==0) false
                    else { field.requestFocus(); anchor=offset(); field.setSelection(anchor!!); true }
                }
                MotionEvent.ACTION_MOVE -> {
                    val start=anchor ?: return@setOnTouchListener false
                    field.setSelection(start.coerceIn(0,field.text.length),offset()); true
                }
                MotionEvent.ACTION_UP -> {
                    val start=anchor ?: return@setOnTouchListener false
                    field.setSelection(start.coerceIn(0,field.text.length),offset())
                    anchor=null; field.performClick(); true
                }
                MotionEvent.ACTION_CANCEL -> { val handled=anchor!=null; anchor=null; handled }
                else -> false
            }
        }
    }
    override fun onResume() { super.onResume(); PopupWindows.fit(this,320,440) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    private fun receive(intent: Intent) {
        val text=when(intent.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            else -> intent.getStringExtra("snapshot")
        }
        if (text!=null) editor.setText(text.take(100_000))
        intent.removeExtra("snapshot"); intent.removeExtra(Intent.EXTRA_TEXT); intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        if (intent.getBooleanExtra("emptySnapshot",false)) Ui.toast(this,"Źródło nie udostępniło tekstu. Wklej go ręcznie lub użyj Udostępnij.")
    }
}
