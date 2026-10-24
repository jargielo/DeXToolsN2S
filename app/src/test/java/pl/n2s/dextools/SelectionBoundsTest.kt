package pl.n2s.dextools

import org.junit.Assert.*
import org.junit.Test

class SelectionBoundsTest {
    @Test fun reverseDragIsNormalized() {
        assertEquals(SelectionBounds(10,20,90,70),SelectionBounds.fromDrag(90f,70f,10f,20f,100,100))
    }
    @Test fun outsideDragClampsToBitmap() {
        assertEquals(SelectionBounds(0,0,100,80),SelectionBounds.fromDrag(-12f,-5f,150f,90f,100,80))
    }
    @Test fun clickAndEmptyImagesAreRejected() {
        assertNull(SelectionBounds.fromDrag(5f,5f,5f,5f,100,100))
        assertNull(SelectionBounds.fromDrag(0f,0f,50f,50f,0,0))
    }
    @Test fun nonFiniteInputIsRejected() {
        assertNull(SelectionBounds.fromDrag(Float.NaN,0f,20f,30f,100,100))
        assertNull(SelectionBounds.fromDrag(0f,0f,Float.POSITIVE_INFINITY,30f,100,100))
    }
    @Test fun fractionalCoordinatesCoverSelectedPixels() {
        assertEquals(SelectionBounds(1,2,11,21),SelectionBounds.fromDrag(1.8f,2.3f,10.1f,20.2f,100,100))
    }
    @Test fun outsideSelectionCannotCreateEmptyCrop() {
        assertNull(SelectionBounds.fromDrag(-20f,5f,-2f,40f,100,100))
    }
}
