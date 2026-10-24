package pl.n2s.dextools

object Shortcuts {
    // KEYCODE_S = 47. Pure policy, independently testable on the JVM.
    fun isCapture(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean, meta: Boolean) =
        keyCode == 47 && ctrl && shift && !alt && !meta
}
