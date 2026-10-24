package pl.n2s.dextools

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TextAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance=this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* No background collection. */ }
    override fun onInterrupt() {}
    override fun onDestroy() { if (instance===this) instance=null; super.onDestroy() }
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (Shortcuts.isCapture(event.keyCode,event.isCtrlPressed,event.isShiftPressed,event.isAltPressed,event.isMetaPressed)) {
            if (event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0) {
                try { CapturePermissionActivity.launch(this,false) }
                catch (_: RuntimeException) { Ui.toast(this,"System zablokował skrót. Użyj panelu pływającego.") }
            }
            return true
        }
        return false
    }
    fun snapshot(): String {
        val root=rootInActiveWindow ?: return ""
        val result=StringBuilder()
        var visited=0
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (++visited>2500 || depth>60 || result.length>=100_000 || node.isPassword || !node.isVisibleToUser) return
            val text=node.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                val remaining=100_000-result.length
                result.append(text.take(remaining)).append('\n')
            } else if (node.childCount==0) {
                node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
                    result.append(it.take(100_000-result.length)).append('\n')
                }
            }
            for (i in 0 until node.childCount) {
                val child=node.getChild(i) ?: continue
                visit(child,depth+1)
                @Suppress("DEPRECATION")
                child.recycle()
                if (visited>2500 || result.length>=100_000) break
            }
        }
        try { visit(root,0) } finally { @Suppress("DEPRECATION") root.recycle() }
        return result.toString().trim()
    }
    fun selectAndCopyFocusedField(): Boolean {
        val root=rootInActiveWindow ?: return false
        val node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        try {
            if (node==null || !node.isEditable || node.isPassword) return false
            val length=node.text?.length ?: return false
            val selection=Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,length)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION,selection) &&
                node.performAction(AccessibilityNodeInfo.ACTION_COPY)
        } finally {
            @Suppress("DEPRECATION")
            node?.recycle()
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }
    companion object { var instance: TextAccessibilityService?=null; private set }
}
