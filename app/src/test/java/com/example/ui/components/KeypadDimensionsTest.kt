package com.example.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeypadDimensionsTest {

    @Test
    fun `standard portrait phone fills full width while capping key height`() {
        val totalWidth = 412.dp
        val horizontalPadding = 12.dp
        val gap = 8.dp
        val dims = calculateKeypadDimensions(
            totalWidth = totalWidth,
            screenHeight = 892.dp,
            horizontalPadding = horizontalPadding,
            gap = gap
        )

        val rowWidth = dims.keyWidth * 4 + gap * 3
        assertEquals(totalWidth - horizontalPadding * 2, rowWidth)
        assertTrue(dims.keyHeight < dims.keyWidth)
        assertEquals(dims.keyHeight * 2 + gap, dims.enterHeight)
    }

    @Test
    fun `tall narrow phone keeps square keys and fills full width`() {
        val totalWidth = 360.dp
        val horizontalPadding = 12.dp
        val gap = 8.dp
        val dims = calculateKeypadDimensions(
            totalWidth = totalWidth,
            screenHeight = 900.dp,
            horizontalPadding = horizontalPadding,
            gap = gap
        )

        val rowWidth = dims.keyWidth * 4 + gap * 3
        assertEquals(totalWidth - horizontalPadding * 2, rowWidth)
        assertEquals(dims.keyWidth, dims.keyHeight)
        assertEquals(dims.keyHeight * 2 + gap, dims.enterHeight)
    }

    @Test
    fun `landscape phone fills full width and clamps height to minimum touch target`() {
        val totalWidth = 800.dp
        val horizontalPadding = 12.dp
        val gap = 8.dp
        val dims = calculateKeypadDimensions(
            totalWidth = totalWidth,
            screenHeight = 360.dp,
            horizontalPadding = horizontalPadding,
            gap = gap
        )

        val rowWidth = dims.keyWidth * 4 + gap * 3
        assertEquals(totalWidth - horizontalPadding * 2, rowWidth)
        assertEquals(44.dp, dims.keyHeight)
        assertEquals(96.dp, dims.enterHeight)
    }
}
