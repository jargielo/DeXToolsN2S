package pl.n2s.dextools

import android.content.*
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*

object ScreenshotStore {
    fun save(context: Context, bitmap: Bitmap): android.net.Uri {
        val name = "DeX_n2s_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT).format(Date())}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,name)
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"${Environment.DIRECTORY_PICTURES}/Screenshots")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)) { "Nie udało się utworzyć pliku" }
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out)) }
            resolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null)
        } catch (e: Exception) { resolver.delete(uri,null,null); throw e }
        return uri
    }
    fun copy(context: Context, uri: android.net.Uri) {
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newUri(context.contentResolver,"Zrzut ekranu — DeX Tools n2s",uri))
    }
}
