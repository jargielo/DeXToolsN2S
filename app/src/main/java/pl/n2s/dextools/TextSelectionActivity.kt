package pl.n2s.dextools

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class TextSelectionActivity : Activity() {
    private lateinit var editor: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout=Ui.column(this)
        layout.addView(Ui.text(this,"TEXT SELECTION  /  n2s",12f,true))
        layout.addView(Ui.title(this,"Zaznacz i skopiuj"))
        layout.addView(Ui.text(this,"Przeciągnij myszą po tekście. Ctrl + A / Ctrl + C lub przyciski poniżej. Możesz również wkleić lub edytować treść. Tekst nie jest zapisywany przez aplikację.",15f,true))
        editor=EditText(this).apply {
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
            hint="Brak tekstu. Użyj T nad aplikacją źródłową, udostępnij tekst albo wklej go tutaj."
            textSize=18f; gravity=Gravity.TOP or Gravity.START
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSelectAllOnFocus(false)
            isSaveEnabled=false
            background=Ui.background()
            setPadding(Ui.dp(context,16),Ui.dp(context,16),Ui.dp(context,16),Ui.dp(context,16))
        }
        layout.addView(editor,LinearLayout.LayoutParams(-1,0,1f))
        layout.addView(Ui.space(this))
        layout.addView(Ui.button(this,"Kopiuj zaznaczenie") {
            val a=minOf(editor.selectionStart,editor.selectionEnd).coerceAtLeast(0)
            val b=maxOf(editor.selectionStart,editor.selectionEnd).coerceAtLeast(0)
            if (a==b) Ui.toast(this,"Najpierw zaznacz tekst myszą")
            else {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Tekst — DeX Tools n2s",editor.text.subSequence(a,b)))
                Ui.toast(this,"Skopiowano zaznaczony tekst")
            }
        },LinearLayout.LayoutParams(-1,-2))
        layout.addView(Ui.space(this))
        layout.addView(Ui.button(this,"Zaznacz wszystko") { editor.requestFocus(); editor.selectAll() },LinearLayout.LayoutParams(-1,-2))
        layout.addView(Ui.space(this))
        layout.addView(Ui.button(this,"Wyczyść tekst") { editor.text.clear() },LinearLayout.LayoutParams(-1,-2))
        Ui.mount(this,layout)
        receive(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    private fun receive(intent: Intent) {
        val text=when(intent.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            else -> intent.getStringExtra("snapshot")
        }
        if (text!=null) editor.setText(text.take(100_000))
        intent.removeExtra("snapshot"); intent.removeExtra(Intent.EXTRA_TEXT); intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        if (intent.getBooleanExtra("emptySnapshot",false)) Ui.toast(this,"Aplikacja nie udostępniła tekstu. Spróbuj Udostępnij lub wklej tekst ręcznie.")
    }
}
