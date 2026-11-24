package com.motonav

import com.motonav.data.model.LatLng
import com.motonav.integrations.whatsapp.LocationLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocationLinkParserTest {

    @Test
    fun testExtractGoogleMapsLinks() {
        val sampleMessage = "Bro check this spot: https://maps.app.goo.gl/abCdEf123 lets meet there at 5pm"
        val extracted = LocationLinkParser.extractLocationLink(sampleMessage)
        assertNotNull(extracted)
        assertEquals("https://maps.app.goo.gl/abCdEf123", extracted)
    }

    @Test
    fun testExtractGeoUri() {
        val sampleGeo = "Meet here geo:22.5726,88.3639"
        val extracted = LocationLinkParser.extractLocationLink(sampleGeo)
        assertNotNull(extracted)
        assertEquals("geo:22.5726,88.3639", extracted)

        val coords = LocationLinkParser.parseCoordinates(extracted!!)
        assertNotNull(coords)
        assertEquals(22.5726, coords!!.latitude, 0.0001)
        assertEquals(88.3639, coords.longitude, 0.0001)
    }

    @Test
    fun testExtractRawCoordinates() {
        val message = "Coordinates for petrol pump: 22.6845, 88.2912 reach fast"
        val extracted = LocationLinkParser.extractLocationLink(message)
        assertNotNull(extracted)

        val coords = LocationLinkParser.parseCoordinates(extracted!!)
        assertNotNull(coords)
        assertEquals(22.6845, coords!!.latitude, 0.0001)
        assertEquals(88.2912, coords.longitude, 0.0001)
    }

    @Test
    fun testNoLocationInMessage() {
        val normalText = "Hey are we still riding to Digha tomorrow morning?"
        val extracted = LocationLinkParser.extractLocationLink(normalText)
        assertNull(extracted)
    }
}
