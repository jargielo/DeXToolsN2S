package pl.n2s.dextools

import org.junit.Assert.*
import org.junit.Test

class ShortcutsTest {
    @Test fun exactShortcutIsAccepted() { assertTrue(Shortcuts.isCapture(47,true,true,false,false)) }
    @Test fun ordinaryTypingAndOtherShortcutsAreNotIntercepted() {
        assertFalse(Shortcuts.isCapture(47,false,true,false,false))
        assertFalse(Shortcuts.isCapture(47,true,false,false,false))
        assertFalse(Shortcuts.isCapture(47,true,true,true,false))
        assertFalse(Shortcuts.isCapture(47,true,true,false,true))
        assertFalse(Shortcuts.isCapture(48,true,true,false,false))
    }
}
