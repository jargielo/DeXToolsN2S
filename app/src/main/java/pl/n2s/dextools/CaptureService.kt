package pl.n2s.dextools

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.*
import android.os.*
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("Screenshot").apply { start() }
    private val worker = Handler(workerThread.looper)
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var latest: Bitmap? = null
    private var overlay: RegionOverlay? = null
    private var region = false
    private var frameWidth = 0; private var frameHeight = 0
    private val completed = AtomicBoolean(false)
    private var stopping = false
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (busy || intent == null) { if (!busy) stopSelf(); return START_NOT_STICKY }
        busy = true
        try {
            startForeground(10, Ui.notification(this,"capture","Zrzut ekranu","Przechwytywanie na Twoje żądanie",Intent(this,CaptureService::class.java).setAction("STOP")),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            region = intent.getBooleanExtra("region",false)
            @Suppress("DEPRECATION")
            val data = intent.getParcelableExtra<Intent>("data") ?: error("Brak zgody MediaProjection")
            projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(intent.getIntExtra("resultCode",0),data)
            projection!!.registerCallback(object: MediaProjection.Callback() {
                override fun onStop() { if (!stopping && !completed.get()) fail("Przechwytywanie zakończone przez system") }
                override fun onCapturedContentResize(width: Int, height: Int) {
                    if (!completed.get() && width > 0 && height > 0 && (width != frameWidth || height != frameHeight)) configureReader(width,height)
                }
            },worker)
            val bounds = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            configureReader(bounds.width(),bounds.height())
            display = projection!!.createVirtualDisplay("DeX screenshot",frameWidth,frameHeight,resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,worker)
            worker.postDelayed({ finishFrame() },650)
            worker.postDelayed({ if (!completed.get()) fail("Brak obrazu. Sprawdź zgodę i ograniczenia aplikacji źródłowej.") },8000)
        } catch (e: Exception) { fail("Nie udało się przechwycić ekranu: ${e.message}") }
        return START_NOT_STICKY
    }
    private fun configureReader(width: Int, height: Int) {
        reader?.close(); latest?.recycle(); latest=null
        frameWidth=width; frameHeight=height
        reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2).also { r ->
            r.setOnImageAvailableListener({ source ->
                try {
                    source.acquireLatestImage()?.use { image ->
                        if (completed.get()) return@use
                        val plane=image.planes[0]
                        check(plane.pixelStride==4) { "Nieobsługiwany format pikseli" }
                        val bitmap=Bitmap.createBitmap(image.width,image.height,Bitmap.Config.ARGB_8888)
                        val sourceBuffer=plane.buffer
                        if (plane.rowStride==image.width*4) bitmap.copyPixelsFromBuffer(sourceBuffer)
                        else {
                            // Some devices omit padding after the last row: copy only actual pixels.
                            val packed=java.nio.ByteBuffer.allocate(image.width*image.height*4)
                            for (row in 0 until image.height) {
                                val pixels=sourceBuffer.duplicate()
                                pixels.position(row*plane.rowStride)
                                pixels.limit(row*plane.rowStride+image.width*4)
                                packed.put(pixels)
                            }
                            packed.flip()
                            bitmap.copyPixelsFromBuffer(packed)
                        }
                        latest?.recycle(); latest=bitmap
                    }
                } catch (e: Exception) { if (!completed.get()) fail("Błąd odczytu obrazu: ${e.message}") }
            },worker)
        }
        display?.resize(width,height,resources.displayMetrics.densityDpi)
        display?.surface=reader!!.surface
    }
    private fun finishFrame() {
        if (completed.get()) return
        val bitmap=latest
        if (bitmap==null) { worker.postDelayed({ finishFrame() },150); return }
        if (!completed.compareAndSet(false,true)) return
        latest=null
        releaseProjection()
        if (region) main.post {
            try {
                overlay=RegionOverlay(this,bitmap) { cropped ->
                    overlay=null
                    if (cropped != null) worker.post { save(cropped); if (cropped !== bitmap) bitmap.recycle() }
                    else { bitmap.recycle(); stopSelf() }
                }.also { it.open() }
            } catch (e: Exception) { bitmap.recycle(); fail("Nie udało się otworzyć zaznaczania: ${e.message}") }
        } else save(bitmap)
    }
    private fun save(bitmap: Bitmap) {
        try {
            val uri=ScreenshotStore.save(this,bitmap)
            main.post {
                try { ScreenshotStore.copy(this,uri); Ui.toast(this,"Zapisano w Screenshots i skopiowano obraz") }
                catch (_: Exception) { Ui.toast(this,"Zapisano w Screenshots. System odmówił dostępu do schowka.") }
                stopSelf()
            }
        } catch (e: Exception) { fail("Nie udało się zapisać zrzutu: ${e.message}") }
        finally { bitmap.recycle() }
    }
    private fun fail(message: String) { main.post { Ui.toast(this,message); stopSelf() } }
    private fun releaseProjection() {
        stopping=true
        display?.release(); display=null
        reader?.close(); reader=null
        projection?.stop(); projection=null
    }
    override fun onDestroy() {
        busy=false
        main.removeCallbacksAndMessages(null)
        overlay?.close(null); overlay=null
        worker.post { completed.set(true); releaseProjection(); latest?.recycle(); latest=null; workerThread.quitSafely() }
        OverlayService.instance?.show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    companion object { @Volatile var busy=false; private set }
}
