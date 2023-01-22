package com.dp.advancedgunnerycontrol.weaponais

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.lwjgl.util.vector.Vector2f
import org.lazywizard.lazylib.ext.plus

internal class AGCUtilsKtTest {

    @Test
    fun testIntersection() {
        val p = Vector2f(0.41844496f, -0.53832567f)
        val dp = Vector2f(0.7005532f, -0.25651577f)
        val r = 0.21049917f
        val dr = 1.086479f

        val actual = intersectionTime(p, dp, r, dr)

        val left = r + dr * actual!!
        val right = (p + dp.times_(actual!!)).length()

        assertEquals(1.2075576f, actual)
        assertEquals(left, right)
    }
}