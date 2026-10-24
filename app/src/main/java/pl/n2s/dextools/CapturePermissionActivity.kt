package pl.n2s.dextools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings

class CapturePermissionActivity : Activity() {
    private var transferred = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CaptureService.busy) { Ui.toast(this, "Zrzut jest już w toku"); finish(); return }
        if (intent.getBooleanExtra("region", false) && !Settings.canDrawOverlays(this)) {
            Ui.toast(this, "Zaznaczanie fragmentu wymaga zgody na nakładkę"); finish(); return
        }
        MainActivity.dismissForCapture()
        OverlayService.instance?.hide()
        if (savedInstanceState == null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            // Request a display rather than single-app sharing: coordinates must match the region overlay.
            val consent = if (Build.VERSION.SDK_INT >= 34)
                manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
            else manager.createScreenCaptureIntent()
            @Suppress("DEPRECATION")
            startActivityForResult(consent, 1)
        }
    }
    @Deprecated("Platform result API used without external UI dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            try {
                startForegroundService(Intent(this, CaptureService::class.java)
                    .putExtra("resultCode", resultCode).putExtra("data", data)
                    .putExtra("region", intent.getBooleanExtra("region", false)))
                transferred = true
            } catch (e: RuntimeException) { Ui.toast(this, "Nie można rozpocząć przechwytywania: ${e.message}") }
        } else Ui.toast(this, "Anulowano zrzut ekranu")
        if (!transferred) OverlayService.instance?.show()
        finish()
    }
    companion object {
        fun launch(context: Context, region: Boolean) {
            context.startActivity(Intent(context, CapturePermissionActivity::class.java)
                .putExtra("region", region).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
