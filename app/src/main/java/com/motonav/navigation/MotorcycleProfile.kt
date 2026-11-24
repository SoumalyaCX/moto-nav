package com.motonav.navigation

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

enum class RidingStyle {
    SPORT,       // Maximizes twisty curvy backroads, heavy penalty on straight highways
    TOURING,     // Balanced scenic riding, good pavement, comfortable pace
    COMMUTE,     // Direct fastest path
    ADVENTURE    // Dual-sport / off-road friendly, allows trails & gravel
}

data class MotorcycleRoutingOptions(
    val style: RidingStyle = RidingStyle.SPORT,
    val avoidHighways: Boolean = true,
    val avoidTolls: Boolean = false,
    val avoidUnpaved: Boolean = true,
    val maxSpeedKmh: Int = 110
) {
    /**
     * Builds GraphHopper Custom Model JSON definition.
     */
    fun toCustomModelJson(): String {
        val priorityArray = mutableListOf<Map<String, String>>()
        val speedArray = mutableListOf<Map<String, String>>()

        // Highway avoidance
        if (avoidHighways || style == RidingStyle.SPORT) {
            priorityArray.add(mapOf("if" to "road_class == MOTORWAY", "multiply_by" to "0.15"))
            priorityArray.add(mapOf("if" to "road_class == TRUNK", "multiply_by" to "0.4"))
        }

        // Motorcycle preference for curvy secondary / tertiary roads
        when (style) {
            RidingStyle.SPORT -> {
                priorityArray.add(mapOf("if" to "road_class == SECONDARY", "multiply_by" to "1.7"))
                priorityArray.add(mapOf("if" to "road_class == TERTIARY", "multiply_by" to "1.6"))
                priorityArray.add(mapOf("if" to "surface == UNPAVED || surface == GRAVEL", "multiply_by" to "0.1"))
            }
            RidingStyle.TOURING -> {
                priorityArray.add(mapOf("if" to "road_class == PRIMARY", "multiply_by" to "1.2"))
                priorityArray.add(mapOf("if" to "road_class == SECONDARY", "multiply_by" to "1.4"))
                priorityArray.add(mapOf("if" to "surface == UNPAVED", "multiply_by" to "0.2"))
            }
            RidingStyle.COMMUTE -> {
                priorityArray.add(mapOf("if" to "road_class == MOTORWAY", "multiply_by" to "1.0"))
            }
            RidingStyle.ADVENTURE -> {
                priorityArray.add(mapOf("if" to "surface == UNPAVED || surface == GRAVEL", "multiply_by" to "1.2"))
                priorityArray.add(mapOf("if" to "road_class == TRACK", "multiply_by" to "1.4"))
            }
        }

        // Toll avoidance
        if (avoidTolls) {
            priorityArray.add(mapOf("if" to "toll == ALL || toll == HGV", "multiply_by" to "0.0"))
        }

        // Surface safety
        if (avoidUnpaved && style != RidingStyle.ADVENTURE) {
            priorityArray.add(mapOf("if" to "surface == UNPAVED || surface == DIRT || surface == EARTH", "multiply_by" to "0.1"))
        }

        // Speed capping
        speedArray.add(mapOf("if" to "road_class == MOTORWAY", "limit_to" to "$maxSpeedKmh"))
        speedArray.add(mapOf("if" to "true", "limit_to" to "90"))

        val model = mapOf(
            "priority" to priorityArray,
            "speed" to speedArray
        )

        return Gson().toJson(model)
    }
}
