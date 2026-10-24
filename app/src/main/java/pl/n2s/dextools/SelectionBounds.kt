package pl.n2s.dextools

import kotlin.math.*

data class SelectionBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
    companion object {
        fun fromDrag(x1: Float, y1: Float, x2: Float, y2: Float, width: Int, height: Int): SelectionBounds? {
            if (width <= 0 || height <= 0 || !listOf(x1,y1,x2,y2).all { it.isFinite() }) return null
            val l = floor(min(x1, x2)).toInt().coerceIn(0, width)
            val t = floor(min(y1, y2)).toInt().coerceIn(0, height)
            val r = ceil(max(x1, x2)).toInt().coerceIn(0, width)
            val b = ceil(max(y1, y2)).toInt().coerceIn(0, height)
            return if (r-l >= 2 && b-t >= 2) SelectionBounds(l,t,r,b) else null
        }
    }
}
