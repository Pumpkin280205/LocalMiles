package com.example.localmiles.Screens.Driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodLocationTest {

    @Test
    fun `same point is zero metres apart`() {
        assertEquals(0.0, distanceMeters(-26.2041, 28.0473, -26.2041, 28.0473), 0.001)
    }

    @Test
    fun `one hundredth of a degree of latitude is about 1112 metres`() {
        assertEquals(1112.0, distanceMeters(-26.20, 28.04, -26.21, 28.04), 5.0)
    }

    @Test
    fun `a spot 100 m away is inside the limit and 300 m away is outside`() {
        // 0.0009 degrees of latitude is about 100 m and 0.0027 is about 300 m
        assertTrue(distanceMeters(-26.2000, 28.04, -26.2009, 28.04) < MAX_POD_DISTANCE_METERS)
        assertTrue(distanceMeters(-26.2000, 28.04, -26.2027, 28.04) > MAX_POD_DISTANCE_METERS)
    }
}
