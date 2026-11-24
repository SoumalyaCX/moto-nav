package com.motonav

import com.motonav.navigation.MotorcycleRoutingOptions
import com.motonav.navigation.RidingStyle
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorcycleProfileTest {

    @Test
    fun testSportProfilePenalizesMotorways() {
        val options = MotorcycleRoutingOptions(
            style = RidingStyle.SPORT,
            avoidHighways = true
        )
        val json = options.toCustomModelJson()

        // Verify motorway priority penalty
        assertTrue(json.contains("road_class == MOTORWAY"))
        assertTrue(json.contains("0.15"))

        // Verify secondary road booster for curves
        assertTrue(json.contains("road_class == SECONDARY"))
        assertTrue(json.contains("1.7"))
    }

    @Test
    fun testTouringProfileBoostsScenicStateRoads() {
        val options = MotorcycleRoutingOptions(
            style = RidingStyle.TOURING,
            avoidHighways = false
        )
        val json = options.toCustomModelJson()

        assertTrue(json.contains("road_class == PRIMARY"))
        assertTrue(json.contains("1.2"))
    }
}
