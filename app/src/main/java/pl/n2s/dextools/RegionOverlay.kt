package pl.n2s.dextools

import android.content.Context
import android.graphics.*
import android.view.*

class RegionOverlay(context: Context, private val bitmap: Bitmap, private val done: (Bitmap?) -> Unit) : View(context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var x0 = 0f; private var y0 = 0f; private var x1 = 0f; private var y1 = 0f
    private var dragging = false
    private var closed = false
    fun open() {
        isFocusableInTouchMode = true
        val p = WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE)
        p.gravity = Gravity.TOP or Gravity.START
        p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        wm.addView(this, p)
        requestFocus()
    }
    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, Rect(0,0,width,height), paint)
        canvas.drawColor(0x77000000)
        if (dragging) {
            val rect = RectF(minOf(x0,x1),minOf(y0,y1),maxOf(x0,x1),maxOf(y0,y1))
            canvas.save(); canvas.clipRect(rect)
            canvas.drawBitmap(bitmap, null, Rect(0,0,width,height), paint); canvas.restore()
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = Ui.accent
            canvas.drawRect(rect,paint); paint.style = Paint.Style.FILL
        }
        paint.color = Color.BLACK
        val textHeight = Ui.dp(context, 64).toFloat()
        canvas.drawRect(0f,0f,width.toFloat(),textHeight,paint)
        paint.color = Color.WHITE; paint.textSize = Ui.dp(context,16).toFloat()
        canvas.drawText("Przeciągnij myszą • Esc / Wstecz: anuluj", Ui.dp(context,16).toFloat(), textHeight*0.65f, paint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (closed) return true
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { x0=event.x; y0=event.y; x1=x0; y1=y0; dragging=true; invalidate() }
            MotionEvent.ACTION_MOVE -> { x1=event.x; y1=event.y; invalidate() }
            MotionEvent.ACTION_UP -> {
                val bounds = SelectionBounds.fromDrag(x0*bitmap.width/width,y0*bitmap.height/height,
                    event.x*bitmap.width/width,event.y*bitmap.height/height,bitmap.width,bitmap.height)
                if (bounds != null) close(Bitmap.createBitmap(bitmap,bounds.left,bounds.top,bounds.width,bounds.height))
                else { dragging=false; invalidate(); Ui.toast(context,"Zaznacz większy prostokąt") }
            }
            MotionEvent.ACTION_CANCEL -> close(null)
        }
        return true
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode==KeyEvent.KEYCODE_ESCAPE || keyCode==KeyEvent.KEYCODE_BACK) { close(null); return true }
        return super.onKeyDown(keyCode,event)
    }
    fun close(result: Bitmap?) {
        if (closed) return
        closed=true
        if (isAttachedToWindow) wm.removeView(this)
        done(result)
    }
}
